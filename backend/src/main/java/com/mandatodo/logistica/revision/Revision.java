package com.mandatodo.logistica.revision;

import com.mandatodo.logistica.pedido.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "revisiones")
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Convert(converter = MotivoRetencionConverter.class)
    @Column(name = "motivo_retencion", nullable = false, length = 50)
    private MotivoRetencion motivoRetencion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resolucion", nullable = false, length = 20)
    private EstadoResolucion estadoResolucion;

    protected Revision() {
    }

    public Revision(Pedido pedido, MotivoRetencion motivoRetencion) {
        this.pedido = pedido;
        this.motivoRetencion = motivoRetencion;
        this.estadoResolucion = EstadoResolucion.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public MotivoRetencion getMotivoRetencion() {
        return motivoRetencion;
    }

    public EstadoResolucion getEstadoResolucion() {
        return estadoResolucion;
    }

    public void setEstadoResolucion(EstadoResolucion estadoResolucion) {
        this.estadoResolucion = estadoResolucion;
    }
}
