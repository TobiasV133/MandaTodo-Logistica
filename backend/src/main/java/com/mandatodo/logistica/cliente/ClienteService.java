package com.mandatodo.logistica.cliente;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.dto.ActualizarClienteDTO;
import com.mandatodo.logistica.cliente.dto.ClienteDTO;
import com.mandatodo.logistica.cliente.dto.ClienteRequest;
import com.mandatodo.logistica.cliente.dto.ClienteResponse;
import com.mandatodo.logistica.cliente.dto.CrearClienteDTO;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;
    private final OperarioRepository operarioRepository;

    public ClienteService(
        ClienteRepository clienteRepository,
        AuditoriaService auditoriaService,
        OperarioRepository operarioRepository
    ) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
        this.operarioRepository = operarioRepository;
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

    @Transactional(readOnly = true)
    public ClienteDTO buscarPorId(Long idCliente) {
        return ClienteDTO.desde(
            clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Cliente no encontrado: " + idCliente
                ))
        );
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

    @Transactional
    public ClienteDTO crear(CrearClienteDTO dto, Long idOperario) {
        String documento = normalizarDocumento(dto.numeroDocumento());
        if (documento.isBlank()) {
            throw new ReglaNegocioException("El número de documento no puede estar vacío.");
        }

        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (clienteRepository.existsByNumeroDocumento(documento)) {
            throw new ReglaNegocioException(
                "Ya existe un cliente con el documento " + documento + "."
            );
        }
        if (clienteRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(
                "Ya existe un cliente con el email " + email + "."
            );
        }

        Cliente cliente = new Cliente(
            normalizarTipoDocumento(dto.tipoDocumento()),
            documento,
            dto.nombreRazonSocial().trim(),
            dto.telefono().trim(),
            email,
            dto.tipoCliente(),
            Boolean.TRUE.equals(dto.esFrecuente())
        );

        Cliente guardado = clienteRepository.save(cliente);
        registrarAuditoria(idOperario, "Alta de cliente: " + documento, TipoEvento.ALTA);
        return ClienteDTO.desde(guardado);
    }

    @Transactional
    public ClienteDTO actualizar(Long idCliente, ActualizarClienteDTO dto, Long idOperario) {
        Cliente cliente = clienteRepository.findById(idCliente)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Cliente no encontrado: " + idCliente
            ));

        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (!cliente.getEmail().equalsIgnoreCase(email)
            && clienteRepository.existsByEmailAndIdNot(email, idCliente)) {
            throw new ReglaNegocioException(
                "El email " + email + " ya está asociado a otro cliente."
            );
        }

        cliente.setNombreRazonSocial(dto.nombreRazonSocial().trim());
        cliente.setTelefono(dto.telefono().trim());
        cliente.setEmail(email);
        cliente.setTipoCliente(dto.tipoCliente());
        cliente.setEsFrecuente(Boolean.TRUE.equals(dto.esFrecuente()));

        Cliente guardado = clienteRepository.save(cliente);
        registrarAuditoria(
            idOperario,
            "Edición de cliente: " + cliente.getNumeroDocumento(),
            TipoEvento.EDICION
        );
        return ClienteDTO.desde(guardado);
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

    private void registrarAuditoria(Long idOperario, String actividad, TipoEvento tipoEvento) {
        if (idOperario == null) {
            throw new ErrorHttpException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al operario responsable."
            );
        }

        Operario auditor = operarioRepository.findById(idOperario)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Operario no encontrado: " + idOperario
            ));

        auditoriaService.registrar(auditor, null, actividad, tipoEvento);
    }
}
