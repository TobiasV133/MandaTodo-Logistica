package com.mandatodo.logistica.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import com.mandatodo.logistica.auditoria.Auditoria;
import com.mandatodo.logistica.auditoria.AuditoriaRepository;
import com.mandatodo.logistica.auditoria.TipoEvento;
import com.mandatodo.logistica.cliente.Cliente;
import com.mandatodo.logistica.cliente.ClienteRepository;
import com.mandatodo.logistica.cliente.TipoCliente;
import com.mandatodo.logistica.operario.EstadoOperario;
import com.mandatodo.logistica.operario.Operario;
import com.mandatodo.logistica.operario.OperarioRepository;
import com.mandatodo.logistica.operario.RolOperario;
import com.mandatodo.logistica.revision.EstadoResolucion;
import com.mandatodo.logistica.revision.MotivoRetencion;
import com.mandatodo.logistica.revision.Revision;
import com.mandatodo.logistica.revision.RevisionRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PersistenciaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OperarioRepository operarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private RevisionRepository revisionRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Test
    void persisteYConsultaElCircuitoBasicoDeUnPedidoRetenido() {
        Cliente cliente = clienteRepository.save(new Cliente(
            "CUIT",
            "30-71234567-8",
            "Distribuidora del Sur S.A.",
            "11 4567-8901",
            "contacto@delsur.com.ar",
            TipoCliente.Empresa,
            true
        ));
        Operario operario = operarioRepository.save(new Operario(
            "Laura Gómez",
            "laura@mandatodo.com",
            "hash",
            RolOperario.Admin,
            EstadoOperario.Activo
        ));

        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 10, 23, 9, 0);
        Pedido pedido = pedidoRepository.save(new Pedido(
            cliente,
            "Carga paletizada",
            new BigDecimal("25.00"),
            new BigDecimal("25000.00"),
            "S/N",
            0,
            null,
            "San Miguel",
            null,
            null,
            LocalDate.of(2026, 10, 23),
            fechaCreacion
        ));
        pedido.setEstado(EstadoPedido.EN_REVISION);

        Revision revision = revisionRepository.save(new Revision(
            pedido,
            MotivoRetencion.DIRECCION_AMBIGUA
        ));

        auditoriaRepository.save(new Auditoria(
            operario,
            pedido,
            "Alta de pedido retenido por Dirección ambigua",
            TipoEvento.ALTA,
            fechaCreacion
        ));
        auditoriaRepository.save(new Auditoria(
            operario,
            pedido,
            "Remito generado para el pedido #" + pedido.getId(),
            TipoEvento.ALTA,
            fechaCreacion.plusHours(1)
        ));

        entityManager.flush();
        entityManager.clear();

        assertThat(clienteRepository.findByNumeroDocumento("30-71234567-8")).isPresent();
        assertThat(pedidoRepository.findConClientePorId(pedido.getId())).isPresent();
        assertThat(pedidoRepository.findDelDia(
            cliente.getId(),
            LocalDate.of(2026, 10, 23).atStartOfDay(),
            LocalDate.of(2026, 10, 24).atStartOfDay()
        )).hasSize(1);
        assertThat(revisionRepository.findByPedidoIdAndEstadoResolucionOrderByIdAsc(
            pedido.getId(),
            EstadoResolucion.PENDIENTE
        )).extracting(Revision::getId).containsExactly(revision.getId());
        assertThat(revisionRepository.findByPedidoIdInOrderByIdAsc(
            java.util.List.of(pedido.getId())
        )).hasSize(1);
        assertThat(auditoriaRepository.findAllByOrderByFechaHoraDesc()).hasSize(2);
        assertThat(auditoriaRepository.countByPedido_IdAndActividadStartingWith(
            pedido.getId(),
            "Remito "
        )).isEqualTo(1);
    }
}
