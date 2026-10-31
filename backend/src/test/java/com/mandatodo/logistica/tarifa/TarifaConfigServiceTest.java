package com.mandatodo.logistica.tarifa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import com.mandatodo.logistica.tarifa.dto.ActualizarTarifaRequest;
import com.mandatodo.logistica.tarifa.dto.TarifaConfigResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TarifaConfigServiceTest {

    @Mock
    private TarifaConfigRepository tarifaConfigRepository;

    @Mock
    private OperarioRepository operarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private TarifaConfigService service;
    private Operario admin;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
        service = new TarifaConfigService(
            tarifaConfigRepository,
            operarioRepository,
            auditoriaService,
            clock
        );
        admin = new Operario(
            "lauragomez",
            "Laura Gómez",
            "laura@mandatodo.com",
            "hash",
            RolOperario.Admin,
            EstadoOperario.Activo
        );
        ReflectionTestUtils.setField(admin, "id", 1L);
    }

    @Test
    void devuelveLosValoresPorDefectoSinConfiguracionGuardada() {
        when(tarifaConfigRepository.findById(TarifaConfig.ID_UNICO))
            .thenReturn(Optional.empty());

        TarifaConfigResponse respuesta = service.obtener();

        assertThat(respuesta.tarifaBase()).isEqualByComparingTo("10000.00");
        assertThat(respuesta.tarifaMedia()).isEqualByComparingTo("30000.00");
        assertThat(respuesta.maxPesoKg()).isEqualByComparingTo("80");
        assertThat(respuesta.maxKilometros()).isEqualByComparingTo("30");
        assertThat(respuesta.fechaUltimaModificacion()).isNull();
        assertThat(service.obtenerValores()).isEqualTo(TarifaValores.POR_DEFECTO);
    }

    @Test
    void unAdministradorPuedeActualizarElTarifarioYAudaLaAuditoria() {
        when(operarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(tarifaConfigRepository.findById(TarifaConfig.ID_UNICO))
            .thenReturn(Optional.empty());
        when(tarifaConfigRepository.save(any(TarifaConfig.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        TarifaConfigResponse respuesta = service.actualizar(
            request(
                "12000.00", "36000.00", "6000.00", "3500.00",
                "6.000", "6.000", "25.000", "18.000", "90.000", "35.000"
            ),
            1L,
            "Admin"
        );

        assertThat(respuesta.tarifaBase()).isEqualByComparingTo("12000.00");
        assertThat(respuesta.tarifaMedia()).isEqualByComparingTo("36000.00");
        assertThat(respuesta.recargoPorKm()).isEqualByComparingTo("6000.00");
        assertThat(respuesta.recargoPorKg()).isEqualByComparingTo("3500.00");
        assertThat(respuesta.limitePesoMedio()).isEqualByComparingTo("25");
        assertThat(respuesta.maxKilometros()).isEqualByComparingTo("35");
        assertThat(respuesta.idOperarioUltimaModificacion()).isEqualTo(1L);
        assertThat(respuesta.fechaUltimaModificacion()).isNotNull();

        verify(auditoriaService).registrar(
            eq(admin),
            isNull(),
            contains("Tarifario actualizado"),
            eq(TipoEvento.EDICION)
        );
    }

    @Test
    void unOperarioNoPuedeModificarElTarifario() {
        assertThatThrownBy(() ->
            service.actualizar(requestPorDefecto(), 2L, "Operario")
        )
            .isInstanceOf(ErrorHttpException.class)
            .satisfies(error -> assertThat(((ErrorHttpException) error).getEstado())
                .isEqualTo(HttpStatus.FORBIDDEN));

        verify(tarifaConfigRepository, never()).save(any(TarifaConfig.class));
        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void rechazaNivelesIncoherentes() {
        assertThatThrownBy(() ->
            service.actualizar(
                request(
                    "10000.00", "30000.00", "5000.00", "3000.00",
                    "10.000", "5.000", "5.000", "15.000", "80.000", "30.000"
                ),
                1L,
                "Admin"
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("límite de peso del nivel base");
    }

    private ActualizarTarifaRequest requestPorDefecto() {
        return request(
            "10000.00", "30000.00", "5000.00", "3000.00",
            "5.000", "5.000", "20.000", "15.000", "80.000", "30.000"
        );
    }

    private ActualizarTarifaRequest request(
        String tarifaBase,
        String tarifaMedia,
        String recargoPorKm,
        String recargoPorKg,
        String limitePesoBase,
        String limiteKmBase,
        String limitePesoMedio,
        String limiteKmMedio,
        String maxPesoKg,
        String maxKilometros
    ) {
        return new ActualizarTarifaRequest(
            new BigDecimal(tarifaBase),
            new BigDecimal(tarifaMedia),
            new BigDecimal(recargoPorKm),
            new BigDecimal(recargoPorKg),
            new BigDecimal(limitePesoBase),
            new BigDecimal(limiteKmBase),
            new BigDecimal(limitePesoMedio),
            new BigDecimal(limiteKmMedio),
            new BigDecimal(maxPesoKg),
            new BigDecimal(maxKilometros)
        );
    }
}
