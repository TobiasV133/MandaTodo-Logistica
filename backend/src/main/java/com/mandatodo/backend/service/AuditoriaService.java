package com.mandatodo.backend.service;

import com.mandatodo.backend.dto.AuditoriaDTO;
import com.mandatodo.backend.exception.ApiException;
import com.mandatodo.backend.model.Auditoria;
import com.mandatodo.backend.model.Operario;
import com.mandatodo.backend.model.TipoEvento;
import com.mandatodo.backend.repository.AuditoriaRepository;
import com.mandatodo.backend.repository.OperarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consulta y registro de la auditoría global e inalterable.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final OperarioRepository operarioRepository;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            OperarioRepository operarioRepository
    ) {
        this.auditoriaRepository = auditoriaRepository;
        this.operarioRepository = operarioRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listar() {
        return auditoriaRepository.findAllByOrderByFechaHoraDesc().stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarPorTipoEvento(TipoEvento tipoEvento) {
        if (tipoEvento == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de evento es obligatorio.");
        }

        return auditoriaRepository.findByTipoEventoOrderByFechaHoraDesc(tipoEvento).stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listarPorOperario(Integer idOperario) {
        if (idOperario == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El operario es obligatorio.");
        }

        return auditoriaRepository.findByOperarioIdOperarioOrderByFechaHoraDesc(idOperario).stream()
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listar(LocalDate fecha, Integer idOperario, TipoEvento tipoEvento) {
        List<Auditoria> eventos;

        if (fecha != null) {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);
            eventos = auditoriaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
        } else if (idOperario != null) {
            eventos = auditoriaRepository.findByOperarioIdOperarioOrderByFechaHoraDesc(idOperario);
        } else {
            eventos = auditoriaRepository.findAllByOrderByFechaHoraDesc();
        }

        return eventos.stream()
                .filter(evento -> idOperario == null
                        || (evento.getOperario() != null
                        && idOperario.equals(evento.getOperario().getIdOperario())))
                .filter(evento -> tipoEvento == null || tipoEvento == evento.getTipoEvento())
                .map(AuditoriaDTO::from)
                .toList();
    }

    @Transactional
    public AuditoriaDTO registrarEvento(
            Integer idOperario,
            Integer idPedido,
            String actividad,
            TipoEvento tipoEvento
    ) {
        if (idOperario == null) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo identificar al operario responsable."
            );
        }
        if (actividad == null || actividad.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La actividad de auditoría es obligatoria.");
        }
        if (tipoEvento == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tipo de evento es obligatorio.");
        }

        Operario operario = operarioRepository.findById(idOperario)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Operario no encontrado: " + idOperario
                ));

        if (!operario.isActivo()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "El operario " + idOperario + " está inactivo y no puede registrar eventos."
            );
        }

        Auditoria auditoria = new Auditoria(
                operario,
                idPedido,
                actividad.trim(),
                tipoEvento,
                LocalDateTime.now()
        );

        return AuditoriaDTO.from(auditoriaRepository.save(auditoria));
    }
}
