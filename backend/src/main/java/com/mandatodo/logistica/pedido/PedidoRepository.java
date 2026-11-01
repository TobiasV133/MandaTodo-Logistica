package com.mandatodo.logistica.pedido;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("select p from Pedido p join fetch p.cliente where p.id = :id")
    Optional<Pedido> findConClientePorId(@Param("id") Long id);

    @Query("select p from Pedido p join fetch p.cliente order by p.fechaCreacion desc")
    List<Pedido> findAllConCliente();

    @Query("select p from Pedido p join fetch p.cliente where p.estado = :estado order by p.fechaCreacion desc")
    List<Pedido> findByEstadoConCliente(@Param("estado") EstadoPedido estado);

    /**
     * Indica si el cliente ya tiene un pedido registrado para la fecha indicada.
     * Se usa para detectar duplicados (mismo cliente, misma fecha programada,
     * asumiendo que es el mismo destino). Se excluyen los pedidos
     * {@code CANCELADO_EN_TRANSITO}.
     */
    boolean existsByClienteIdAndFechaProgramadaAndEstadoNot(
        Long clienteId,
        LocalDate fechaProgramada,
        EstadoPedido estadoExcluido
    );

    long countByEstado(EstadoPedido estado);

    long countByCodigoPedidoStartingWith(String prefijo);
}
