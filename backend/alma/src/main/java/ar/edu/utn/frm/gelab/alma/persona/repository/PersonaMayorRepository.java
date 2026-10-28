package ar.edu.utn.frm.gelab.alma.persona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;

public interface PersonaMayorRepository extends JpaRepository<PersonaMayor, Long> {

    Optional<PersonaMayor> findByCuentaId(Long cuentaId);
}
