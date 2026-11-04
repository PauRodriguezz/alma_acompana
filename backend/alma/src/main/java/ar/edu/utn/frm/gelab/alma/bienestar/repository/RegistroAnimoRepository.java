package ar.edu.utn.frm.gelab.alma.bienestar.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.bienestar.domain.RegistroAnimo;

/**
 * Las consultas llevan la cuenta del token: nunca se busca un registro sin la persona.
 */
public interface RegistroAnimoRepository extends JpaRepository<RegistroAnimo, Long> {

    /** El registro más reciente de la persona en [desde, hasta): por fecha y hora y, si empatan, por id. */
    Optional<RegistroAnimo> findFirstByPersonaMayorCuentaIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDescIdDesc(
            Long cuentaId, LocalDateTime desde, LocalDateTime hasta);
}
