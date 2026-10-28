package com.mandatodo.logistica.pedido;

import java.time.LocalDateTime;
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

    @Query("""
        select p from Pedido p
        where p.cliente.id = :clienteId
          and p.fechaCreacion >= :desde
          and p.fechaCreacion < :hasta
        """)
    List<Pedido> findDelDia(
        @Param("clienteId") Long clienteId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );

    long countByEstado(EstadoPedido estado);
}
