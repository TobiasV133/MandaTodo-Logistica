package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.cliente.ClienteRequest;
import com.mandatodo.backend.dto.cliente.ClienteResponse;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.Cliente;
import com.mandatodo.backend.repository.ClienteRepository;
import com.mandatodo.backend.util.Direcciones;
import com.mandatodo.backend.util.Documentos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorDocumento(String documento) {
        String normalizado = Documentos.normalizar(documento);
        if (normalizado.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresá un número de documento para buscar.");
        }

        return clienteRepository.findByNumeroDocumento(normalizado)
                .map(ClienteResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un cliente con el documento " + normalizado + "."
                ));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    /**
     * Si el documento ya existe, actualiza los datos de contacto. Si no, crea el cliente.
     * Se ejecuta dentro de la transacción del alta del pedido.
     */
    @Transactional
    public Cliente obtenerOCrear(ClienteRequest request) {
        String documento = Documentos.normalizarYValidar(request.numeroDocumento(), request.tipoDocumento());
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();

        Optional<Cliente> porDocumento = clienteRepository.findByNumeroDocumento(documento);
        Optional<Cliente> porEmail = clienteRepository.findByEmailIgnoreCase(email);

        if (porEmail.isPresent()
                && (porDocumento.isEmpty()
                || !porEmail.get().getIdCliente().equals(porDocumento.get().getIdCliente()))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El email " + email + " ya está asociado a otro cliente."
            );
        }

        Cliente cliente = porDocumento.orElseGet(Cliente::new);
        cliente.setTipoDocumento(request.tipoDocumento());
        cliente.setNumeroDocumento(documento);
        cliente.setNombreRazonSocial(request.nombreRazonSocial().trim());
        cliente.setTelefono(Direcciones.limpiarOpcional(request.telefono()));
        cliente.setEmail(email);
        cliente.setTipoCliente(request.tipoCliente());
        cliente.setEsFrecuente(Boolean.TRUE.equals(request.esFrecuente()));

        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public long contar() {
        return clienteRepository.count();
    }
}
