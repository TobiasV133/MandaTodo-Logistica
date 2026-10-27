package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.ActualizarClienteDTO;
import com.mandatodo.backend.dto.ClienteDTO;
import com.mandatodo.backend.dto.CrearClienteDTO;
import com.mandatodo.backend.dto.cliente.ClienteRequest;
import com.mandatodo.backend.dto.cliente.ClienteResponse;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.Cliente;
import com.mandatodo.backend.model.TipoDocumento;
import com.mandatodo.backend.model.TipoEvento;
import com.mandatodo.backend.repository.ClienteRepository;
import com.mandatodo.backend.util.Direcciones;
import com.mandatodo.backend.util.Documentos;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(
            ClienteRepository clienteRepository,
            AuditoriaService auditoriaService
    ) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
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

    @Transactional(readOnly = true)
    public ClienteDTO buscarPorId(Integer idCliente) {
        return clienteRepository.findById(idCliente)
                .map(ClienteDTO::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado: " + idCliente
                ));
    }

    /**
     * Si el documento ya existe, actualiza los datos de contacto. Si no, crea el cliente.
     * Se ejecuta dentro de la transacción del alta del pedido.
     */
    @Transactional
    public Cliente obtenerOCrear(ClienteRequest request) {
        String documento = Documentos.normalizarYValidar(request.numeroDocumento(), request.tipoDocumento());
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);

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

    @Transactional
    public ClienteDTO crear(CrearClienteDTO dto, Integer idOperario) {
        String documento = dto.numeroDocumento().trim();
        String email = normalizarEmail(dto.email());

        if (clienteRepository.existsByNumeroDocumento(documento)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya existe un cliente con el documento " + documento + "."
            );
        }
        if (clienteRepository.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya existe un cliente con el email " + email + "."
            );
        }

        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(parseTipoDocumento(dto.tipoDocumento()));
        cliente.setNumeroDocumento(documento);
        cliente.setNombreRazonSocial(dto.nombreRazonSocial().trim());
        cliente.setTelefono(dto.telefono().trim());
        cliente.setEmail(email);
        cliente.setTipoCliente(dto.tipoCliente());
        cliente.setEsFrecuente(Boolean.TRUE.equals(dto.esFrecuente()));

        Cliente guardado = clienteRepository.save(cliente);

        auditoriaService.registrarEvento(
                idOperario,
                null,
                "Alta de cliente: " + documento,
                TipoEvento.ALTA
        );

        return ClienteDTO.from(guardado);
    }

    @Transactional
    public ClienteDTO actualizar(
            Integer idCliente,
            ActualizarClienteDTO dto,
            Integer idOperario
    ) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado: " + idCliente
                ));

        String email = normalizarEmail(dto.email());
        boolean emailCambio = cliente.getEmail() == null
                || !cliente.getEmail().equalsIgnoreCase(email);

        if (emailCambio && clienteRepository.existsByEmailAndIdClienteNot(email, idCliente)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El email " + email + " ya está asociado a otro cliente."
            );
        }

        cliente.setNombreRazonSocial(dto.nombreRazonSocial().trim());
        cliente.setTelefono(dto.telefono().trim());
        cliente.setEmail(email);
        cliente.setTipoCliente(dto.tipoCliente());
        cliente.setEsFrecuente(Boolean.TRUE.equals(dto.esFrecuente()));

        Cliente guardado = clienteRepository.save(cliente);

        auditoriaService.registrarEvento(
                idOperario,
                null,
                "Edición de cliente: " + cliente.getNumeroDocumento(),
                TipoEvento.EDICION
        );

        return ClienteDTO.from(guardado);
    }

    @Transactional(readOnly = true)
    public long contar() {
        return clienteRepository.count();
    }

    private static TipoDocumento parseTipoDocumento(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de documento es obligatorio.");
        }

        try {
            return TipoDocumento.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de documento inválido: " + valor
            );
        }
    }

    private static String normalizarEmail(String email) {
        if (email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El email es obligatorio.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
