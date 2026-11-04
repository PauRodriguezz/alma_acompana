package ar.edu.utn.frm.gelab.alma.juegos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frm.gelab.alma.juegos.domain.IntentoEjercicio;

public interface IntentoEjercicioRepository extends JpaRepository<IntentoEjercicio, Long> {

    /** Intentos de la persona en [desde, hasta): con 00:00 de hoy y 00:00 de mañana da la ronda de hoy. */
    long countByPersonaMayorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(Long personaMayorId,
            LocalDateTime desde, LocalDateTime hasta);

    /** Último intento de la persona por cada ejercicio que jugó alguna vez. */
    @Query("""
            select new ar.edu.utn.frm.gelab.alma.juegos.repository.UltimoIntento(
                i.ejercicio.id, max(i.fechaHora))
            from IntentoEjercicio i
            where i.personaMayor.id = :personaMayorId and i.ejercicio is not null
            group by i.ejercicio.id
            """)
    List<UltimoIntento> ultimoIntentoPorEjercicio(@Param("personaMayorId") Long personaMayorId);
}
