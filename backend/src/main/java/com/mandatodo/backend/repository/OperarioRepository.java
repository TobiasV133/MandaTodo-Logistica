package com.mandatodo.backend.repository;

import com.mandatodo.backend.model.EstadoOperario;
import com.mandatodo.backend.model.Operario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para los operarios del sistema.
 */
public interface OperarioRepository extends JpaRepository<Operario, Integer> {

    /**
     * Búsqueda explícita de login por username, sin depender del case-sensitivity
     * de la columna ni de la inferencia de nombres de Spring Data.
     */
    @Query("SELECT o FROM Operario o WHERE LOWER(o.username) = LOWER(:username)")
    Optional<Operario> findByUsernameIgnoreCase(@Param("username") String username);

    /**
     * Búsqueda explícita de login por email, usada como fallback de compatibilidad.
     */
    @Query("SELECT o FROM Operario o WHERE LOWER(o.email) = LOWER(:email)")
    Optional<Operario> findByEmailIgnoreCase(@Param("email") String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdOperarioNot(String username, Integer idOperario);

    Optional<Operario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdOperarioNot(String email, Integer idOperario);

    List<Operario> findByEstado(EstadoOperario estado);

    List<Operario> findAllByOrderByNombreAsc();

    List<Operario> findByEstadoOrderByNombreAsc(EstadoOperario estado);
}
