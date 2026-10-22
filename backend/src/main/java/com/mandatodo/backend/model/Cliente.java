package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "clientes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_clientes_numero_documento", columnNames = "numero_documento"),
                @UniqueConstraint(name = "uk_clientes_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, columnDefinition = "enum('DNI','CUIT')")
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "nombre_razon_social", nullable = false, length = 180)
    private String nombreRazonSocial;

    @Column(name = "telefono", length = 40)
    private String telefono;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente", nullable = false, columnDefinition = "enum('Particular','Empresa')")
    private TipoCliente tipoCliente;

    @Column(name = "es_frecuente", nullable = false)
    private Boolean esFrecuente = Boolean.FALSE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (esFrecuente == null) {
            esFrecuente = Boolean.FALSE;
        }
    }
}
