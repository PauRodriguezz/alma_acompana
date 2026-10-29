package ar.edu.utn.frm.gelab.alma.microcapsulas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;

/**
 * "Vigente" = sin fechaBaja.
 */
public interface MicrocapsulaRepository extends JpaRepository<Microcapsula, Long> {

    List<Microcapsula> findByFechaBajaIsNullOrderByFechaPublicacionDescIdDesc();

    Optional<Microcapsula> findByIdAndFechaBajaIsNull(Long id);

    long countByFechaBajaIsNull();

    /** Incluye las dadas de baja: lo usa el seed para no recrear lo que se dio de baja. */
    boolean existsByTitulo(String titulo);
}
