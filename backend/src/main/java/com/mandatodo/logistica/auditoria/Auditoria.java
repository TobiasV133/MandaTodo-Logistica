package com.mandatodo.logistica.auditoria;

import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.pedido.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Registro global e inalterable. A nivel de base de datos, los triggers
 * trg_auditoria_no_update y trg_auditoria_no_delete bloquean UPDATE y DELETE.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_operario", nullable = false)
    private Operario operario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido")
    private Pedido pedido;

    @Column(name = "actividad", nullable = false, length = 500)
    private String actividad;

    @Convert(converter = TipoEventoConverter.class)
    @Column(name = "tipo_evento", nullable = false, length = 30)
    private TipoEvento tipoEvento;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    protected Auditoria() {
    }

    public Auditoria(
        Operario operario,
        Pedido pedido,
        String actividad,
        TipoEvento tipoEvento,
        LocalDateTime fechaHora
    ) {
        this.operario = operario;
        this.pedido = pedido;
        this.actividad = actividad;
        this.tipoEvento = tipoEvento;
        this.fechaHora = fechaHora;
    }

    public Long getId() {
        return id;
    }

    public Operario getOperario() {
        return operario;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public String getActividad() {
        return actividad;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
