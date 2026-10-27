package com.mandatodo.backend.dto;

import com.mandatodo.backend.model.Auditoria;

import java.time.LocalDateTime;

/**
 * Datos de salida de un evento de auditoría.
 */
public record AuditoriaDTO(
        Long idAuditoria,
        String actividad,
        String tipoEvento,
        LocalDateTime fechaHora,
        Integer idOperario,
        Integer idPedido
) {

    public static AuditoriaDTO from(Auditoria auditoria) {
        return new AuditoriaDTO(
                auditoria.getIdAuditoria(),
                auditoria.getActividad(),
                auditoria.getTipoEvento() == null ? null : auditoria.getTipoEvento().valorApi(),
                auditoria.getFechaHora(),
                auditoria.getIdOperario(),
                auditoria.getIdPedido()
        );
    }
}
