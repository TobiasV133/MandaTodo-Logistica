package com.mandatodo.logistica.tarifa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila única del tarifario editable ({@code tarifa_config}, id = 1).
 *
 * <p>Si no existe la fila, el sistema usa {@link TarifaValores#POR_DEFECTO}. La primera
 * edición desde la pantalla de pedidos crea el registro.</p>
 */
@Entity
@Table(name = "tarifa_config")
public class TarifaConfig {

    public static final Integer ID_UNICO = 1;

    @Id
    @Column(name = "id_tarifa", nullable = false)
    private Integer id;

    @Column(name = "tarifa_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaBase;

    @Column(name = "tarifa_media", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaMedia;

    @Column(name = "recargo_por_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargoPorKm;

    @Column(name = "recargo_por_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargoPorKg;

    @Column(name = "limite_peso_base", nullable = false, precision = 10, scale = 3)
    private BigDecimal limitePesoBase;

    @Column(name = "limite_km_base", nullable = false, precision = 10, scale = 3)
    private BigDecimal limiteKmBase;

    @Column(name = "limite_peso_medio", nullable = false, precision = 10, scale = 3)
    private BigDecimal limitePesoMedio;

    @Column(name = "limite_km_medio", nullable = false, precision = 10, scale = 3)
    private BigDecimal limiteKmMedio;

    @Column(name = "max_peso_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal maxPesoKg;

    @Column(name = "max_kilometros", nullable = false, precision = 10, scale = 3)
    private BigDecimal maxKilometros;

    @Column(name = "id_operario_ultima_modificacion")
    private Long idOperarioUltimaModificacion;

    @Column(name = "fecha_ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

    protected TarifaConfig() {
    }

    public TarifaConfig(Integer id) {
        this.id = id;
    }

    public void actualizar(
        TarifaValores valores,
        Long idOperario,
        LocalDateTime fechaModificacion
    ) {
        this.tarifaBase = valores.tarifaBase();
        this.tarifaMedia = valores.tarifaMedia();
        this.recargoPorKm = valores.recargoPorKm();
        this.recargoPorKg = valores.recargoPorKg();
        this.limitePesoBase = valores.limitePesoBase();
        this.limiteKmBase = valores.limiteKmBase();
        this.limitePesoMedio = valores.limitePesoMedio();
        this.limiteKmMedio = valores.limiteKmMedio();
        this.maxPesoKg = valores.maxPesoKg();
        this.maxKilometros = valores.maxKilometros();
        this.idOperarioUltimaModificacion = idOperario;
        this.fechaUltimaModificacion = fechaModificacion;
    }

    public TarifaValores aValores() {
        return new TarifaValores(
            tarifaBase,
            tarifaMedia,
            recargoPorKm,
            recargoPorKg,
            limitePesoBase,
            limiteKmBase,
            limitePesoMedio,
            limiteKmMedio,
            maxPesoKg,
            maxKilometros
        );
    }

    public Integer getId() {
        return id;
    }

    public Long getIdOperarioUltimaModificacion() {
        return idOperarioUltimaModificacion;
    }

    public LocalDateTime getFechaUltimaModificacion() {
        return fechaUltimaModificacion;
    }
}
