package ar.edu.utn.frm.gelab.alma.administrador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.administrador.domain.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByCuentaId(Long cuentaId);
}
