package ar.edu.utn.frm.gelab.alma.juegos.repository;

import java.time.LocalDateTime;

/**
 * Fecha y hora del último intento de una persona en un ejercicio (proyección de IntentoEjercicioRepository).
 */
public record UltimoIntento(Long ejercicioId, LocalDateTime fechaHora) {
}
