package ar.edu.utn.frm.gelab.alma.juegos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frm.gelab.alma.juegos.domain.ProgresoJuego;

public interface ProgresoJuegoRepository extends JpaRepository<ProgresoJuego, Long> {

    Optional<ProgresoJuego> findByPersonaMayorId(Long personaMayorId);
}
