package com.mandatodo.logistica.operario;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.dto.ActualizarUsuarioDTO;
import com.mandatodo.logistica.operario.dto.CrearUsuarioDTO;
import com.mandatodo.logistica.operario.dto.OperarioDTO;
import com.mandatodo.logistica.security.PasswordService;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import com.mandatodo.logistica.shared.error.ReglaNegocioException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return operarioRepository.findAllByOrderByNombreAsc()
            .stream()
            .map(OperarioDTO::desde)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OperarioDTO> listarActivos() {
        return operarioRepository.findByEstadoOrderByNombreAsc(EstadoOperario.Activo)
            .stream()
            .map(OperarioDTO::desde)
            .toList();
    }

    @Transactional(readOnly = true)
    public OperarioDTO buscarPorId(Long idOperario) {
        return OperarioDTO.desde(buscarEntidad(idOperario));
    }

    @Transactional
    public OperarioDTO crear(CrearUsuarioDTO dto, Long idOperarioAuditor) {
        String username = normalizarUsername(dto.username());
        String email = normalizarEmail(dto.email());

        if (operarioRepository.existsByUsername(username)) {
            throw new ReglaNegocioException(
                "Ya existe un operario con el username " + username + "."
            );
        }
        if (operarioRepository.existsByEmail(email)) {
            throw new ReglaNegocioException(
                "Ya existe un operario con el email " + email + "."
            );
        }

        Operario operario = new Operario(
            username,
            dto.nombre().trim(),
            email,
            hashearPassword(dto.password()),
            dto.rol(),
            EstadoOperario.Activo
        );

        Operario guardado = operarioRepository.save(operario);
        registrarAuditoria(idOperarioAuditor, "Alta de operario: " + username, TipoEvento.ALTA);
        return OperarioDTO.desde(guardado);
    }

    @Transactional
    public OperarioDTO actualizar(
        Long idOperario,
        ActualizarUsuarioDTO dto,
        Long idOperarioAuditor
    ) {
        Operario operario = buscarEntidad(idOperario);
        String username = normalizarUsername(dto.username());
        String email = normalizarEmail(dto.email());

        if (!operario.getUsername().equalsIgnoreCase(username)
            && operarioRepository.existsByUsernameAndIdNot(username, idOperario)) {
            throw new ReglaNegocioException(
                "El username " + username + " ya está asociado a otro operario."
            );
        }
        if (!operario.getEmail().equalsIgnoreCase(email)
            && operarioRepository.existsByEmailAndIdNot(email, idOperario)) {
            throw new ReglaNegocioException(
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
        registrarAuditoria(idOperarioAuditor, "Edición de operario: " + username, TipoEvento.EDICION);
        return OperarioDTO.desde(guardado);
    }

    private Operario buscarEntidad(Long idOperario) {
        if (idOperario == null) {
            throw new RecursoNoEncontradoException("Operario no encontrado.");
        }
        return operarioRepository.findById(idOperario)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Operario no encontrado: " + idOperario
            ));
    }

    private void registrarAuditoria(Long idOperarioAuditor, String actividad, TipoEvento tipoEvento) {
        if (idOperarioAuditor == null) {
            throw new ErrorHttpException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al operario responsable."
            );
        }
        Operario auditor = buscarEntidad(idOperarioAuditor);
        auditoriaService.registrar(auditor, null, actividad, tipoEvento);
    }

    private String hashearPassword(String rawPassword) {
        if (rawPassword == null) {
            throw new ReglaNegocioException("La contraseña es obligatoria.");
        }
        byte[] bytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 72) {
            throw new ReglaNegocioException(
                "La contraseña no puede superar los 72 bytes (límite de BCrypt)."
            );
        }
        return passwordService.hash(rawPassword);
    }

    private static String normalizarUsername(String username) {
        if (username == null) {
            throw new ReglaNegocioException("El username es obligatorio.");
        }
        String normalizado = username.trim().toLowerCase(Locale.ROOT);
        if (normalizado.length() < 4 || normalizado.length() > 30) {
            throw new ReglaNegocioException(
                "El username debe tener entre 4 y 30 caracteres."
            );
        }
        return normalizado;
    }

    private static String normalizarEmail(String email) {
        if (email == null) {
            throw new ReglaNegocioException("El email es obligatorio.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
