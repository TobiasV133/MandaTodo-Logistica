package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.revision.Revision;
import com.mandatodo.logistica.revision.dto.RevisionResumen;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PedidoResponseFactory {

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
            resumenes
        );
    }
}
