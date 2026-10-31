package com.mandatodo.logistica.tarifa;

import com.mandatodo.logistica.auditoria.AuditoriaService;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.shared.error.ErrorHttpException;
import com.mandatodo.logistica.tarifa.dto.ActualizarTarifaRequest;
import com.mandatodo.logistica.tarifa.dto.TarifaConfigResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura y edición del tarifario. La escritura está reservada a administradores.
 */
@Service
public class TarifaConfigService {

    private static final String ROL_ADMIN = "Admin";

    private final TarifaConfigRepository tarifaConfigRepository;
    private final OperarioRepository operarioRepository;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    public TarifaConfigService(
        TarifaConfigRepository tarifaConfigRepository,
        OperarioRepository operarioRepository,
        AuditoriaService auditoriaService,
        Clock clock
    ) {
        this.tarifaConfigRepository = tarifaConfigRepository;
        this.operarioRepository = operarioRepository;
        this.auditoriaService = auditoriaService;
        this.clock = clock;
    }

    /**
     * Devuelve la configuración vigente. Si todavía no se guardó ninguna fila,
     * responde los valores por defecto.
     */
    @Transactional(readOnly = true)
    public TarifaConfigResponse obtener() {
        return tarifaConfigRepository.findById(TarifaConfig.ID_UNICO)
            .map(TarifaConfigResponse::desde)
            .orElseGet(TarifaConfigResponse::porDefecto);
    }

    @Transactional(readOnly = true)
    public TarifaValores obtenerValores() {
        return tarifaConfigRepository.findById(TarifaConfig.ID_UNICO)
            .map(TarifaConfig::aValores)
            .orElse(TarifaValores.POR_DEFECTO);
    }

    @Transactional
    public TarifaConfigResponse actualizar(
        ActualizarTarifaRequest request,
        Long idOperario,
        String rol
    ) {
        if (!esAdministrador(rol)) {
            throw new ErrorHttpException(
                HttpStatus.FORBIDDEN,
                "Solo un administrador puede modificar el tarifario."
            );
        }
        if (idOperario == null) {
            throw new ErrorHttpException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al operario autenticado."
            );
        }

        TarifaValores valores = request.aValores();
        validar(valores);

        Operario operario = operarioRepository.findById(idOperario)
            .orElseThrow(() -> new ErrorHttpException(
                HttpStatus.UNAUTHORIZED,
                "El operario autenticado no existe."
            ));

        TarifaConfig config = tarifaConfigRepository.findById(TarifaConfig.ID_UNICO)
            .orElseGet(() -> new TarifaConfig(TarifaConfig.ID_UNICO));
        config.actualizar(valores, operario.getId(), LocalDateTime.now(clock));
        TarifaConfig guardada = tarifaConfigRepository.save(config);

        auditoriaService.registrar(
            operario,
            null,
            describir(valores),
            TipoEvento.EDICION
        );

        return TarifaConfigResponse.desde(guardada);
    }

    private boolean esAdministrador(String rol) {
        return rol != null && ROL_ADMIN.equalsIgnoreCase(rol.trim());
    }

    /**
     * Evita configuraciones incoherentes: los niveles deben crecer y el máximo debe
     * quedar por encima del nivel medio.
     */
    private void validar(TarifaValores valores) {
        if (valores.tarifaMedia().compareTo(valores.tarifaBase()) < 0) {
            throw new IllegalArgumentException(
                "La tarifa media no puede ser menor que la tarifa base."
            );
        }
        if (valores.limitePesoBase().compareTo(valores.limitePesoMedio()) >= 0) {
            throw new IllegalArgumentException(
                "El límite de peso del nivel base debe ser menor que el del nivel medio."
            );
        }
        if (valores.limiteKmBase().compareTo(valores.limiteKmMedio()) >= 0) {
            throw new IllegalArgumentException(
                "El límite de kilómetros del nivel base debe ser menor que el del nivel medio."
            );
        }
        if (valores.limitePesoMedio().compareTo(valores.maxPesoKg()) >= 0) {
            throw new IllegalArgumentException(
                "El peso máximo debe ser mayor que el límite de peso del nivel medio."
            );
        }
        if (valores.limiteKmMedio().compareTo(valores.maxKilometros()) >= 0) {
            throw new IllegalArgumentException(
                "El máximo de kilómetros debe ser mayor que el límite del nivel medio."
            );
        }
    }

    private String describir(TarifaValores valores) {
        return "Tarifario actualizado: base $" + dinero(valores.tarifaBase())
            + ", media $" + dinero(valores.tarifaMedia())
            + ", km extra $" + dinero(valores.recargoPorKm())
            + ", kg extra $" + dinero(valores.recargoPorKg())
            + "; niveles " + numero(valores.limitePesoBase()) + "/"
            + numero(valores.limiteKmBase()) + " y "
            + numero(valores.limitePesoMedio()) + "/"
            + numero(valores.limiteKmMedio())
            + "; máximo " + numero(valores.maxPesoKg()) + " kg / "
            + numero(valores.maxKilometros()) + " km.";
    }

    private String dinero(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}
