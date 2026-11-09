package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.time.LocalDate;

/**
 * Un trabajo de extracción (contrato, 5.4): trae todo lo que hace falta, leído antes de encolarlo, para que el
 * hilo de la cola no toque PostgreSQL. {@code edadDesde} y {@code edadHasta} son nulas si la pregunta no tiene
 * franja. Lleva el texto de la persona: nunca se escribe en un log (5.9), por eso {@code toString} no lo muestra.
 */
public record TrabajoDeExtraccion(Long personaId, String nombre, String apellido, LocalDate fechaNacimiento,
        int preguntaId, String textoPregunta, Integer edadDesde, Integer edadHasta, String respuesta) {

    @Override
    public String toString() {
        return "TrabajoDeExtraccion[persona=" + personaId + ", pregunta=" + preguntaId + "]";
    }
}
