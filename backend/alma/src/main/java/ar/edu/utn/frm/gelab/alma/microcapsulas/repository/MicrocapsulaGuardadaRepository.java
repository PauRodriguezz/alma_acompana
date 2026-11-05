package ar.edu.utn.frm.gelab.alma.microcapsulas.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.MicrocapsulaGuardada;

public interface MicrocapsulaGuardadaRepository extends JpaRepository<MicrocapsulaGuardada, Long> {

    boolean existsByPersonaMayorIdAndMicrocapsulaId(Long personaMayorId, Long microcapsulaId);

    /** Borra la marca de la persona sobre esa microcápsula, si la hay. Devuelve cuántas filas borró. */
    @Modifying
    @Query("""
            delete from MicrocapsulaGuardada g
            where g.personaMayor.id = :personaMayorId and g.microcapsula.id = :microcapsulaId
            """)
    int borrar(@Param("personaMayorId") Long personaMayorId, @Param("microcapsulaId") Long microcapsulaId);

    /** Guardadas de la persona, vigentes o no, de la última guardada a la primera, con su microcápsula. */
    @Query("""
            select g from MicrocapsulaGuardada g join fetch g.microcapsula
            where g.personaMayor.id = :personaMayorId
            order by g.fechaGuardado desc, g.id desc
            """)
    List<MicrocapsulaGuardada> guardadasDe(@Param("personaMayorId") Long personaMayorId);

    /** Ids de las microcápsulas que la persona tiene guardadas (para el campo "guardada" del listado). */
    @Query("select g.microcapsula.id from MicrocapsulaGuardada g where g.personaMayor.id = :personaMayorId")
    Set<Long> idsGuardadosPor(@Param("personaMayorId") Long personaMayorId);
}
