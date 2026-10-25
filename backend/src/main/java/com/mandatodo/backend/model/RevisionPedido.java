package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Retenciones de la bandeja "En revisión" (DER: tabla revisiones_pedidos).
 */
@Entity
@Table(name = "revisiones_pedidos")
@Getter
@Setter
@NoArgsConstructor
public class RevisionPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision")
    private Integer idRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "motivo_retencion", nullable = false, length = 255)
    private String motivoRetencion;

    @Column(name = "resuelto", nullable = false)
    private Boolean resuelto = Boolean.FALSE;

    @Column(name = "fecha_revision", nullable = false, updatable = false)
    private LocalDateTime fechaRevision;

    @PrePersist
    void prePersist() {
        if (fechaRevision == null) {
            fechaRevision = LocalDateTime.now();
        }
        if (resuelto == null) {
            resuelto = Boolean.FALSE;
        }
    }
}
