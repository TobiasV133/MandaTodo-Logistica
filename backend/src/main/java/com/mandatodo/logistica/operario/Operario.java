package com.mandatodo.logistica.operario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "operarios")
public class Operario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operario")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolOperario rol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoOperario estado;

    protected Operario() {
    }

    /**
     * Constructor histórico usado por tests y flujos previos.
     * Deriva el username del email para no romper esas construcciones.
     */
    public Operario(
        String nombre,
        String email,
        String passwordHash,
        RolOperario rol,
        EstadoOperario estado
    ) {
        this.username = normalizarUsernameDesdeEmail(email);
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.estado = estado;
    }

    public Operario(
        String username,
        String nombre,
        String email,
        String passwordHash,
        RolOperario rol,
        EstadoOperario estado
    ) {
        this.username = username;
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.estado = estado;
    }

    private static String normalizarUsernameDesdeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String valor = email.trim().toLowerCase();
        return valor.length() > 30 ? valor.substring(0, 30) : valor;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RolOperario getRol() {
        return rol;
    }

    public void setRol(RolOperario rol) {
        this.rol = rol;
    }

    public EstadoOperario getEstado() {
        return estado;
    }

    public void setEstado(EstadoOperario estado) {
        this.estado = estado;
    }
}
