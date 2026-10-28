package com.mandatodo.logistica.auditoria;

import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.pedido.Pedido;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final Clock clock;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, Clock clock) {
        this.auditoriaRepository = auditoriaRepository;
        this.clock = clock;
    }

    public void registrar(Operario operario, Pedido pedido, String actividad, TipoEvento tipoEvento) {
        auditoriaRepository.save(new Auditoria(
            operario,
            pedido,
            actividad,
            tipoEvento,
            LocalDateTime.now(clock)
        ));
    }
}
