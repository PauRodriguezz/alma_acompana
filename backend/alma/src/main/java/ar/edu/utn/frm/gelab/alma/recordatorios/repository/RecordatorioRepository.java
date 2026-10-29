package ar.edu.utn.frm.gelab.alma.recordatorios.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.Recordatorio;

/**
 * Todas las consultas llevan la cuenta del token: un recordatorio ajeno nunca se encuentra.
 */
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    Optional<Recordatorio> findByIdAndPersonaMayorCuentaId(Long id, Long cuentaId);

    boolean existsByPersonaMayorCuentaId(Long cuentaId);

    /** Todos los recordatorios de la persona, solo de las clases indicadas, por id ascendente. */
    @Query("""
            select r from Recordatorio r
            where r.personaMayor.cuenta.id = :cuentaId and type(r) in :clases
            order by r.id
            """)
    List<Recordatorio> listar(@Param("cuentaId") Long cuentaId,
            @Param("clases") Collection<Class<? extends Recordatorio>> clases);
}
