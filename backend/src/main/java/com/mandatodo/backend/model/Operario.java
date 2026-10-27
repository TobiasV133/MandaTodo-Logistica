package com.mandatodo.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.util.Objects;

/**
 * Operario responsable de las acciones registradas en el sistema.
 *
 * <p>El {@code username} es el identificador único de inicio de sesión.
 * Se conservan alias de compatibilidad para {@code nombreCompleto} y
 * {@code activo}.</p>
 */
@Entity
@Table(
        name = "operarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_operarios_username", columnNames = "username"),
                @UniqueConstraint(name = "uq_operarios_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Operario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operario", nullable = false, updatable = false)
    private Integer idOperario;

    @Column(name = "username", nullable = false, length = 30)
    private String username;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Convert(converter = RolConverter.class)
    @Column(name = "rol", nullable = false, columnDefinition = "enum('Admin','Operario')")
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "enum('Activo','Inactivo')")
    private EstadoOperario estado = EstadoOperario.Activo;

    @PrePersist
    void prePersist() {
        if (estado == null) {
            estado = EstadoOperario.Activo;
        }
    }

    /**
     * Alias de compatibilidad para el nombre completo original.
     */
    public String getNombreCompleto() {
        return nombre;
    }

    /**
     * Alias de compatibilidad para el nombre completo original.
     */
    public void setNombreCompleto(String nombreCompleto) {
        this.nombre = nombreCompleto;
    }

    /**
     * Alias de compatibilidad para el estado activo original.
     */
    public Boolean getActivo() {
        return isActivo();
    }

    /**
     * Alias de compatibilidad para el estado activo original.
     */
    public void setActivo(Boolean activo) {
        this.estado = Boolean.TRUE.equals(activo)
                ? EstadoOperario.Activo
                : EstadoOperario.Inactivo;
    }

    /**
     * Indica si el operario se encuentra habilitado para operar.
     */
    public boolean isActivo() {
        return estado == EstadoOperario.Activo;
    }

    /**
     * Mantiene el estado canónico no nulo.
     */
    public void setEstado(EstadoOperario estado) {
        this.estado = estado == null ? EstadoOperario.Activo : estado;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Operario operario)) {
            return false;
        }
        if (idOperario != null && operario.idOperario != null) {
            return Objects.equals(idOperario, operario.idOperario);
        }
        return username != null && Objects.equals(username, operario.username);
    }

    @Override
    public int hashCode() {
        if (idOperario != null) {
            return Objects.hash(idOperario);
        }
        return username != null ? Objects.hash(username) : 0;
    }
}
