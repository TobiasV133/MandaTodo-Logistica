package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
        name = "operarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_operarios_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_operarios_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Operario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operario")
    private Integer idOperario;

    @Column(name = "username", nullable = false, length = 60)
    private String username;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = RolConverter.class)
    @Column(name = "rol", nullable = false, columnDefinition = "enum('Admin','Operario')")
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @PrePersist
    void prePersist() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
        if (activo == null) {
            activo = Boolean.TRUE;
        }
    }
}
