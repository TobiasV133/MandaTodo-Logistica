package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Evento de la auditoría global e inalterable de MandáTodo.
 *
 * <p>Mapea exactamente la tabla {@code auditoria} definida en {@code schema.sql}.
 * El operario es obligatorio por la restricción {@code NOT NULL} del DER.</p>
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria", nullable = false, updatable = false)
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_operario", nullable = false)
    private Operario operario;

    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "actividad", nullable = false, length = 500)
    private String actividad;

    @Convert(converter = TipoEventoConverter.class)
    @Column(
            name = "tipo_evento",
            nullable = false,
            columnDefinition = "enum('Alta','Edición','Reimpresión','Cambio de Estado')"
    )
    private TipoEvento tipoEvento;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    /**
     * Constructor requerido por JPA.
     */
    public Auditoria() {
    }

    /**
     * Constructor de conveniencia para pruebas y registros manuales.
     */
    public Auditoria(
            Operario operario,
            Integer idPedido,
            String actividad,
            TipoEvento tipoEvento,
            LocalDateTime fechaHora
    ) {
        this.operario = operario;
        this.idPedido = idPedido;
        this.actividad = actividad;
        this.tipoEvento = tipoEvento;
        this.fechaHora = fechaHora;
    }

    @PrePersist
    void prePersist() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    public Long getIdAuditoria() {
        return idAuditoria;
    }

    public void setIdAuditoria(Long idAuditoria) {
        this.idAuditoria = idAuditoria;
    }

    public Operario getOperario() {
        return operario;
    }

    public void setOperario(Operario operario) {
        this.operario = operario;
    }

    /**
     * Alias de lectura del identificador del operario asociado.
     */
    public Integer getIdOperario() {
        return operario != null ? operario.getIdOperario() : null;
    }

    public Integer getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(Integer idPedido) {
        this.idPedido = idPedido;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Auditoria auditoria)) {
            return false;
        }
        return idAuditoria != null && Objects.equals(idAuditoria, auditoria.idAuditoria);
    }

    @Override
    public int hashCode() {
        return idAuditoria != null ? Objects.hash(idAuditoria) : 0;
    }
}
