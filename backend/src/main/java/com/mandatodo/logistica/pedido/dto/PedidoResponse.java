package com.mandatodo.logistica.pedido.dto;

import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.pedido.EstadoPedido;
import com.mandatodo.logistica.pedido.MotivoFalla;
import com.mandatodo.logistica.revision.dto.RevisionResumen;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
    Long idPedido,
    Long idCliente,
    String cliente,
    TipoCliente tipoCliente,
    String descripcion,
    BigDecimal pesoKg,
    BigDecimal montoEnvio,
    EstadoPedido estado,
    MotivoFalla motivoFalla,
    String motivoCancelacion,
    String calle,
    Integer altura,
    String pisoDepto,
    String localidad,
    BigDecimal latitud,
    BigDecimal longitud,
    LocalDateTime fechaCreacion,
    LocalDate fechaProgramada,
    List<RevisionResumen> revisiones,

    // Campos derivados que conserva el contrato del frontend de main.
    String codigoPedido,
    String nombreCliente,
    String numeroDocumento,
    Boolean clienteFrecuente,
    String descripcionPaquete,
    String direccionCompleta,
    Boolean esDireccionIncompleta,
    String motivoRevision,
    Boolean esReintento,
    BigDecimal precioTotal,
    BigDecimal precioBase,
    BigDecimal recargo,
    BigDecimal kilometros,
    LocalDate fechaProgramadaEntrega,
    String observacion
) {
    public PedidoResponse {
        revisiones = revisiones == null ? List.of() : List.copyOf(revisiones);
    }
}
