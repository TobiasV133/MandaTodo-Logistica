package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pedidos",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pedidos_codigo_pedido", columnNames = "codigo_pedido")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "codigo_pedido", nullable = false, length = 30)
    private String codigoPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "id_pedido_origen")
    private Integer idPedidoOrigen;

    @Column(name = "descripcion_paquete", nullable = false, length = 300)
    private String descripcionPaquete;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "kilometros", nullable = false, precision = 10, scale = 2)
    private BigDecimal kilometros;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "recargo", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargo;

    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "calle", nullable = false, length = 150)
    private String calle;

    @Column(name = "altura")
    private Integer altura;

    @Column(name = "piso_depto", length = 40)
    private String pisoDepto;

    @Column(name = "localidad", nullable = false, length = 120)
    private String localidad;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "enum('EN_REVISION','PENDIENTE','EN_TRANSITO','CANCELADO_EN_TRANSITO','ENTREGADO','FALLIDO')")
    private EstadoPedido estado;

    @Column(name = "motivo_falla", length = 300)
    private String motivoFalla;

    @Column(name = "es_reintento", nullable = false)
    private Boolean esReintento = Boolean.FALSE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_programada_entrega")
    private LocalDate fechaProgramadaEntrega;

    @Column(name = "id_operario_ultima_modificacion")
    private Integer idOperarioUltimaModificacion;

    @Column(name = "fecha_ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = now;
        }
        if (fechaUltimaModificacion == null) {
            fechaUltimaModificacion = now;
        }
        if (esReintento == null) {
            esReintento = Boolean.FALSE;
        }
        if (recargo == null) {
            recargo = BigDecimal.ZERO;
        }
    }
}
