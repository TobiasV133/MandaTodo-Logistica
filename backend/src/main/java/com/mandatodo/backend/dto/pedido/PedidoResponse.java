package com.mandatodo.backend.dto.pedido;

import com.mandatodo.backend.model.EstadoPedido;
import com.mandatodo.backend.model.TipoCliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PedidoResponse(
        Integer idPedido,
        String codigoPedido,
        Integer idCliente,
        String nombreCliente,
        String numeroDocumento,
        TipoCliente tipoCliente,
        Boolean clienteFrecuente,
        String descripcionPaquete,
        BigDecimal pesoKg,
        BigDecimal kilometros,
        BigDecimal precioBase,
        BigDecimal recargo,
        BigDecimal precioTotal,
        String calle,
        Integer altura,
        String pisoDepto,
        String localidad,
        String direccionCompleta,
        BigDecimal latitud,
        BigDecimal longitud,
        EstadoPedido estado,
        boolean esDireccionIncompleta,
        String motivoFalla,
        boolean esReintento,
        LocalDateTime fechaCreacion,
        LocalDate fechaProgramadaEntrega,
        String observacion,
        String motivoRevision
) {
}
