package com.mandatodo.logistica.cliente;

import com.mandatodo.logistica.cliente.dto.ClienteRequest;
import com.mandatodo.logistica.cliente.dto.ClienteResponse;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByNombreRazonSocialAsc()
            .stream()
            .map(ClienteResponse::desde)
            .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String documento) {
        return ClienteResponse.desde(buscarEntidadPorDocumento(documento));
    }

    @Transactional
    public Cliente obtenerOCrear(ClienteRequest request) {
        String documento = normalizarDocumento(request.numeroDocumento());
        if (documento.isBlank()) {
            throw new ReglaNegocioException("El número de documento no puede estar vacío.");
        }

        return clienteRepository.findByDocumentoNormalizado(documento)
            .orElseGet(() -> clienteRepository.save(new Cliente(
                normalizarTipoDocumento(request.tipoDocumento()),
                documento,
                request.nombreRazonSocial().trim(),
                StringUtils.hasText(request.telefono()) ? request.telefono().trim() : "",
                request.email().trim().toLowerCase(Locale.ROOT),
                request.tipoCliente(),
                Boolean.TRUE.equals(request.esFrecuente())
            )));
    }

    private Cliente buscarEntidadPorDocumento(String documento) {
        String normalizado = normalizarDocumento(documento);
        return clienteRepository.findByDocumentoNormalizado(normalizado)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró un cliente con el documento " + documento
            ));
    }

    public static String normalizarDocumento(String documento) {
        return documento == null ? "" : documento.replaceAll("[^0-9]", "");
    }

    private String normalizarTipoDocumento(String tipoDocumento) {
        if (!StringUtils.hasText(tipoDocumento)) {
            throw new ReglaNegocioException("El tipo de documento es obligatorio.");
        }
        String normalizado = tipoDocumento.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.equals("DNI") && !normalizado.equals("CUIT")) {
            throw new ReglaNegocioException("El tipo de documento debe ser DNI o CUIT.");
        }
        return normalizado;
    }
}
