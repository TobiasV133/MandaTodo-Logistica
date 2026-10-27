package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.ActualizarUsuarioDTO;
import com.mandatodo.backend.dto.CrearUsuarioDTO;
import com.mandatodo.backend.dto.OperarioDTO;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.TipoEvento;
import com.mandatodo.backend.repository.OperarioRepository;
import com.mandatodo.backend.security.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Consultas y ABM de operarios.
 */
@Service
public class OperarioService {

    private final OperarioRepository operarioRepository;
    private final PasswordService passwordService;
    private final AuditoriaService auditoriaService;

    public OperarioService(
            OperarioRepository operarioRepository,
            PasswordService passwordService,
            AuditoriaService auditoriaService
    ) {
        this.operarioRepository = operarioRepository;
        this.passwordService = passwordService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<OperarioDTO> listar() {
        return operarioRepository.findAllByOrderByNombreAsc().stream()
                .map(this::convertirToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperarioDTO> listarActivos() {
        return operarioRepository.findByEstadoOrderByNombreAsc(EstadoOperario.Activo).stream()
                .map(this::convertirToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public OperarioDTO buscarPorId(Integer idOperario) {
        return operarioRepository.findById(idOperario)
                .map(this::convertirToDTO)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Operario no encontrado: " + idOperario
                ));
    }

    @Transactional
    public OperarioDTO crear(CrearUsuarioDTO dto, Integer idOperarioAuditor) {
        String username = normalizarUsername(dto.username());
        String email = normalizarEmail(dto.email());

        if (operarioRepository.existsByUsername(username)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya existe un operario con el username " + username + "."
            );
        }

        if (operarioRepository.existsByEmail(email)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ya existe un operario con el email " + email + "."
            );
        }

        Operario operario = new Operario();
        operario.setUsername(username);
        operario.setNombre(dto.nombre().trim());
        operario.setEmail(email);
        operario.setPasswordHash(hashearPassword(dto.password()));
        operario.setRol(dto.rol());
        operario.setEstado(EstadoOperario.Activo);

        Operario guardado = operarioRepository.save(operario);

        auditoriaService.registrarEvento(
                idOperarioAuditor,
                null,
                "Alta de operario: " + username,
                TipoEvento.ALTA
        );

        return convertirToDTO(guardado);
    }

    @Transactional
    public OperarioDTO actualizar(
            Integer idOperario,
            ActualizarUsuarioDTO dto,
            Integer idOperarioAuditor
    ) {
        Operario operario = operarioRepository.findById(idOperario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Operario no encontrado: " + idOperario
                ));

        String username = normalizarUsername(dto.username());
        String email = normalizarEmail(dto.email());

        boolean usernameCambio = operario.getUsername() == null
                || !operario.getUsername().equalsIgnoreCase(username);

        if (usernameCambio
                && operarioRepository.existsByUsernameAndIdOperarioNot(username, idOperario)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El username " + username + " ya está asociado a otro operario."
            );
        }

        boolean emailCambio = operario.getEmail() == null
                || !operario.getEmail().equalsIgnoreCase(email);

        if (emailCambio
                && operarioRepository.existsByEmailAndIdOperarioNot(email, idOperario)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El email " + email + " ya está asociado a otro operario."
            );
        }

        operario.setUsername(username);
        operario.setNombre(dto.nombre().trim());
        operario.setEmail(email);
        operario.setRol(dto.rol());
        operario.setEstado(dto.estado());

        if (dto.password() != null && !dto.password().isBlank()) {
            operario.setPasswordHash(hashearPassword(dto.password()));
        }

        Operario guardado = operarioRepository.save(operario);

        auditoriaService.registrarEvento(
                idOperarioAuditor,
                null,
                "Edición de operario: " + username,
                TipoEvento.EDICION
        );

        return convertirToDTO(guardado);
    }

    /**
     * Mapea la entidad al DTO de salida incluyendo el username.
     */
    private OperarioDTO convertirToDTO(Operario operario) {
        return new OperarioDTO(
                operario.getIdOperario(),
                operario.getUsername(),
                operario.getNombre(),
                operario.getEmail(),
                operario.getRol(),
                operario.getEstado()
        );
    }

    private String hashearPassword(String rawPassword) {
        byte[] bytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña no puede superar los 72 bytes (límite de BCrypt)."
            );
        }
        return passwordService.hash(rawPassword);
    }

    private static String normalizarUsername(String username) {
        if (username == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El username es obligatorio.");
        }

        String normalizado = username.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() < 4 || normalizado.length() > 30) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El username debe tener entre 4 y 30 caracteres."
            );
        }
        return normalizado;
    }

    private static String normalizarEmail(String email) {
        if (email == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El email es obligatorio.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
