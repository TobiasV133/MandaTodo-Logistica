package com.mandatodo.logistica.auditoria.dto;

import com.mandatodo.logistica.auditoria.Auditoria;
import java.time.LocalDateTime;

public record AuditoriaDTO(
    Long idAuditoria,
    String actividad,
    String tipoEvento,
    LocalDateTime fechaHora,
    Long idOperario,
    Long idPedido
) {
    public static AuditoriaDTO desde(Auditoria auditoria) {
        return new AuditoriaDTO(
            auditoria.getId(),
            auditoria.getActividad(),
            auditoria.getTipoEvento() == null ? null : auditoria.getTipoEvento().getValor(),
            auditoria.getFechaHora(),
            auditoria.getOperario() == null ? null : auditoria.getOperario().getId(),
            auditoria.getPedido() == null ? null : auditoria.getPedido().getId()
        );
    }
}
