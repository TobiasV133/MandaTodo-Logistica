package com.mandatodo.logistica.pedido;

import com.mandatodo.logistica.cliente.Cliente;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @Column(name = "codigo_pedido", nullable = false, unique = true, length = 30)
    private String codigoPedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "descripcion_paquete", nullable = false, length = 300)
    private String descripcionPaquete;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "monto_envio", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEnvio;

    @Column(name = "kilometros", nullable = false, precision = 10, scale = 2)
    private BigDecimal kilometros;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "recargo", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargo;

    @Column(name = "precio_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado;

    @Convert(converter = MotivoFallaConverter.class)
    @Column(name = "motivo_falla", length = 50)
    private MotivoFalla motivoFalla;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "calle", nullable = false, length = 200)
    private String calle;

    @Column(name = "altura", nullable = false)
    private Integer altura;

    @Column(name = "piso_depto", length = 50)
    private String pisoDepto;

    @Column(name = "localidad", nullable = false, length = 150)
    private String localidad;

    @Column(name = "latitud", precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    protected Pedido() {
    }

    public Pedido(
        Cliente cliente,
        String descripcion,
        BigDecimal pesoKg,
        BigDecimal kilometros,
        BigDecimal montoEnvio,
        BigDecimal precioBase,
        BigDecimal recargo,
        BigDecimal precioTotal,
        String calle,
        Integer altura,
        String pisoDepto,
        String localidad,
        BigDecimal latitud,
        BigDecimal longitud,
        LocalDate fechaProgramada,
        LocalDateTime fechaCreacion
    ) {
        this.cliente = cliente;
        this.descripcion = descripcion;
        this.descripcionPaquete = descripcion;
        this.pesoKg = pesoKg;
        this.kilometros = kilometros;
        this.montoEnvio = montoEnvio;
        this.precioBase = precioBase;
        this.recargo = recargo;
        this.precioTotal = precioTotal;
        this.calle = calle;
        this.altura = altura;
        this.pisoDepto = pisoDepto;
        this.localidad = localidad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaProgramada = fechaProgramada;
        this.fechaCreacion = fechaCreacion;
        this.estado = EstadoPedido.PENDIENTE;
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (codigoPedido == null || codigoPedido.isBlank()) {
            String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int sufijo = ThreadLocalRandom.current().nextInt(1_000_000);
            codigoPedido = "MT-" + fecha + "-" + String.format("%06d", sufijo);
        }
    }

    public Long getId() {
        return id;
    }

    public String getCodigoPedido() {
        return codigoPedido;
    }

    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getDescripcionPaquete() {
        return descripcionPaquete;
    }

    public void setDescripcionPaquete(String descripcionPaquete) {
        this.descripcionPaquete = descripcionPaquete;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getMontoEnvio() {
        return montoEnvio;
    }

    public BigDecimal getKilometros() {
        return kilometros;
    }

    public void setKilometros(BigDecimal kilometros) {
        this.kilometros = kilometros;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public BigDecimal getRecargo() {
        return recargo;
    }

    public void setRecargo(BigDecimal recargo) {
        this.recargo = recargo;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public void setPrecioTotal(BigDecimal precioTotal) {
        this.precioTotal = precioTotal;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public MotivoFalla getMotivoFalla() {
        return motivoFalla;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public String getCalle() {
        return calle;
    }

    public Integer getAltura() {
        return altura;
    }

    public String getPisoDepto() {
        return pisoDepto;
    }

    public String getLocalidad() {
        return localidad;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDate getFechaProgramada() {
        return fechaProgramada;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public void setMotivoFalla(MotivoFalla motivoFalla) {
        this.motivoFalla = motivoFalla;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public void setAltura(Integer altura) {
        this.altura = altura;
    }

    public void setPisoDepto(String pisoDepto) {
        this.pisoDepto = pisoDepto;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }

    public void setMontoEnvio(BigDecimal montoEnvio) {
        this.montoEnvio = montoEnvio;
    }

    public void setFechaProgramada(LocalDate fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }
}
