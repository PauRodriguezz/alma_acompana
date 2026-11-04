package ar.edu.utn.frm.gelab.alma.ejercicios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * "Activo" = sin fechaBaja.
 */
public interface EjercicioRepository extends JpaRepository<Ejercicio, Long> {

    List<Ejercicio> findByFechaBajaIsNullOrderByFechaAltaDescIdDesc();

    List<Ejercicio> findByCategoriaAndFechaBajaIsNullOrderByFechaAltaDescIdDesc(CategoriaEjercicio categoria);

    /** Activos, del más nuevo al más viejo; si categoria es null, de todas las categorías. */
    default List<Ejercicio> listarActivos(CategoriaEjercicio categoria) {
        return categoria == null
                ? findByFechaBajaIsNullOrderByFechaAltaDescIdDesc()
                : findByCategoriaAndFechaBajaIsNullOrderByFechaAltaDescIdDesc(categoria);
    }

    Optional<Ejercicio> findByIdAndFechaBajaIsNull(Long id);

    long countByFechaBajaIsNull();

    /** Activos de un nivel: el pool del que elige el juego (contrato de juegos, 5.1). */
    List<Ejercicio> findByNivelDificultadAndFechaBajaIsNull(NivelDificultad nivelDificultad);

    /** Incluye los dados de baja: lo usa el seed para no recrear lo que se eliminó. */
    boolean existsByConsigna(String consigna);
}
