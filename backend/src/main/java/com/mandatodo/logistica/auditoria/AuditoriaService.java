package com.mandatodo.logistica.auditoria;

import com.mandatodo.logistica.auditoria.dto.AuditoriaDTO;
import com.mandatodo.logistica.auditoria.dto.RegistrarAuditoriaRequest;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.pedido.Pedido;
import com.mandatodo.logistica.pedido.PedidoRepository;
import com.mandatodo.logistica.shared.error.RecursoNoEncontradoException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final OperarioRepository operarioRepository;
    private final PedidoRepository pedidoRepository;
    private final Clock clock;

    public AuditoriaService(
        AuditoriaRepository auditoriaRepository,
        OperarioRepository operarioRepository,
        PedidoRepository pedidoRepository,
        Clock clock
    ) {
        this.auditoriaRepository = auditoriaRepository;
        this.operarioRepository = operarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.clock = clock;
    }

    @Transactional
    public void registrar(Operario operario, Pedido pedido, String actividad, TipoEvento tipoEvento) {
        auditoriaRepository.save(new Auditoria(
            operario,
            pedido,
            actividad,
            tipoEvento,
            LocalDateTime.now(clock)
        ));
    }

    /**
     * Alta manual de un evento (POST /api/auditoria). La tabla sigue siendo
     * append-only: no existe endpoint de edición ni de borrado.
     */
    @Transactional
    public AuditoriaDTO registrar(RegistrarAuditoriaRequest request) {
        Operario operario = operarioRepository.findById(request.idOperario())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No se encontró el operario " + request.idOperario()
            ));

        Pedido pedido = request.idPedido() == null
            ? null
            : pedidoRepository.findById(request.idPedido())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No se encontró el pedido " + request.idPedido()
                ));

        Auditoria auditoria = auditoriaRepository.save(new Auditoria(
            operario,
            pedido,
            request.actividad().trim(),
            TipoEvento.desde(request.tipoEvento()),
            LocalDateTime.now(clock)
        ));
        return AuditoriaDTO.desde(auditoria);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listar() {
        return listar(null, null, null, null, null);
    }

    /**
     * Listado con filtros del contrato: `fecha` puntual o rango
     * `desde`/`hasta` (inclusive), más operario y tipo de evento.
     */
    @Transactional(readOnly = true)
    public List<AuditoriaDTO> listar(
        LocalDate fecha,
        LocalDate desde,
        LocalDate hasta,
        Long idOperario,
        String tipoEvento
    ) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                "La fecha desde no puede ser posterior a la fecha hasta."
            );
        }

        LocalDate inicio = fecha != null ? fecha : desde;
        LocalDate fin = fecha != null ? fecha : hasta;

        LocalDateTime desdeTimestamp = inicio == null ? null : inicio.atStartOfDay();
        LocalDateTime hastaTimestamp = fin == null ? null : fin.plusDays(1).atStartOfDay();

        return auditoriaRepository.buscar(
                desdeTimestamp,
                hastaTimestamp,
                idOperario,
                TipoEvento.desde(tipoEvento)
            )
            .stream()
            .map(AuditoriaDTO::desde)
            .toList();
    }
}
