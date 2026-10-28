package ar.edu.utn.frm.gelab.alma.recordatorios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.Recordatorio;

/**
 * Todas las consultas llevan la cuenta del token: un recordatorio ajeno nunca se encuentra.
 */
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {

    Optional<Recordatorio> findByIdAndPersonaMayorCuentaId(Long id, Long cuentaId);

    boolean existsByPersonaMayorCuentaId(Long cuentaId);
}
