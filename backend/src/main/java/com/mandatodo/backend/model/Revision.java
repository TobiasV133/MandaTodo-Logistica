package com.mandatodo.backend.model;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Retención de un pedido en la bandeja de seguridad.
 *
 * <p>Mapea la tabla canónica {@code revisiones}.</p>
 */
@Entity
@Table(name = "revisiones")
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision", nullable = false, updatable = false)
    private Integer idRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Convert(converter = MotivoRetencionConverter.class)
    @Column(
            name = "motivo_retencion",
            nullable = false,
            columnDefinition = "enum('Dirección ambigua','Posible duplicado comercial')"
    )
    private MotivoRetencion motivoRetencion;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "estado_resolucion",
            nullable = false,
            columnDefinition = "enum('PENDIENTE','APROBADO','RECHAZADO')"
    )
    private EstadoResolucion estadoResolucion = EstadoResolucion.PENDIENTE;

    /**
     * Constructor requerido por JPA.
     */
    public Revision() {
    }

    /**
     * Constructor de conveniencia para retener un pedido.
     */
    public Revision(Pedido pedido, MotivoRetencion motivoRetencion) {
        this.pedido = pedido;
        this.motivoRetencion = motivoRetencion;
    }

    @PrePersist
    void prePersist() {
        if (estadoResolucion == null) {
            estadoResolucion = EstadoResolucion.PENDIENTE;
        }
    }

    public Integer getIdRevision() {
        return idRevision;
    }

    public void setIdRevision(Integer idRevision) {
        this.idRevision = idRevision;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    /**
     * Alias de lectura del identificador del pedido retenido.
     */
    public Integer getIdPedido() {
        return pedido != null ? pedido.getIdPedido() : null;
    }

    public MotivoRetencion getMotivoRetencion() {
        return motivoRetencion;
    }

    public void setMotivoRetencion(MotivoRetencion motivoRetencion) {
        this.motivoRetencion = motivoRetencion;
    }

    public EstadoResolucion getEstadoResolucion() {
        return estadoResolucion;
    }

    public void setEstadoResolucion(EstadoResolucion estadoResolucion) {
        this.estadoResolucion = estadoResolucion;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Revision revision)) {
            return false;
        }
        return idRevision != null && Objects.equals(idRevision, revision.idRevision);
    }

    @Override
    public int hashCode() {
        return idRevision != null ? Objects.hash(idRevision) : 0;
    }
}
