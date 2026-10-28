package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.revision.EstadoResolucion;
import com.mandatodo.logistica.revision.Revision;
import com.mandatodo.logistica.revision.dto.RevisionResumen;
import com.mandatodo.logistica.shared.geo.Direcciones;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PedidoResponseFactory {

    private static final DateTimeFormatter FORMATO_CODIGO =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    public PedidoResponse crear(Pedido pedido, List<Revision> revisiones) {
        List<RevisionResumen> resumenes = revisiones == null
            ? List.of()
            : revisiones.stream()
                .map(revision -> new RevisionResumen(
                    revision.getId(),
                    revision.getMotivoRetencion(),
                    revision.getEstadoResolucion()
                ))
                .toList();

        String motivoRevision = resumenes.stream()
            .filter(revision -> revision.estadoResolucion() == EstadoResolucion.PENDIENTE)
            .map(revision -> revision.motivoRetencion().getValor())
            .distinct()
            .collect(Collectors.joining(" y "));
        if (motivoRevision.isBlank()) {
            motivoRevision = null;
        }

        String codigoPedido = "MT-"
            + (pedido.getFechaCreacion() == null
                ? "SIN-FECHA"
                : pedido.getFechaCreacion().format(FORMATO_CODIGO))
            + "-"
            + String.format("%06d", pedido.getId() == null ? 0L : pedido.getId());

        return new PedidoResponse(
            pedido.getId(),
            pedido.getCliente().getId(),
            pedido.getCliente().getNombreRazonSocial(),
            pedido.getCliente().getTipoCliente(),
            pedido.getDescripcion(),
            pedido.getPesoKg(),
            pedido.getMontoEnvio(),
            pedido.getEstado(),
            pedido.getMotivoFalla(),
            pedido.getMotivoCancelacion(),
            pedido.getCalle(),
            pedido.getAltura(),
            pedido.getPisoDepto(),
            pedido.getLocalidad(),
            pedido.getLatitud(),
            pedido.getLongitud(),
            pedido.getFechaCreacion(),
            pedido.getFechaProgramada(),
            resumenes,

            codigoPedido,
            pedido.getCliente().getNombreRazonSocial(),
            pedido.getCliente().getNumeroDocumento(),
            pedido.getCliente().isEsFrecuente(),
            pedido.getDescripcion(),
            Direcciones.formatear(
                pedido.getCalle(),
                pedido.getAltura(),
                pedido.getPisoDepto(),
                pedido.getLocalidad()
            ),
            Direcciones.esIncompleta(
                pedido.getCalle(),
                pedido.getAltura(),
                pedido.getLocalidad()
            ),
            motivoRevision,
            false,
            pedido.getMontoEnvio(),
            null,
            null,
            null,
            pedido.getFechaProgramada(),
            null
        );
    }
}
