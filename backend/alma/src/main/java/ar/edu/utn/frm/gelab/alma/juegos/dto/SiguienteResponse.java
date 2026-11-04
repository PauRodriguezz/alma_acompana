package ar.edu.utn.frm.gelab.alma.juegos.dto;

/**
 * Respuesta de GET /api/juegos/siguiente (contrato de juegos, 6.1). posicion va de 1 a totalRonda.
 */
public record SiguienteResponse(EjercicioJuegoResponse ejercicio, int posicion, int totalRonda) {
}
