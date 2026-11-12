package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;

/**
 * Un trabajo de extracción (contrato, 5.4): trae todo lo que hace falta, leído antes de encolarlo, para que el
 * hilo de la cola no toque PostgreSQL. {@code edadDesde} y {@code edadHasta} son nulas si la pregunta no tiene
 * franja (y en una repregunta, que nunca estima). {@code repregunta} es nula en una respuesta del guion; en una
 * repregunta (5.7) lleva el recuerdo y el dato que completa, y {@code textoPregunta} es el texto fijo de esa
 * repregunta. Lleva el texto de la persona: nunca se escribe en un log (5.9), por eso {@code toString} no lo
 * muestra.
 */
public record TrabajoDeExtraccion(Long personaId, String nombre, String apellido, LocalDate fechaNacimiento,
        int preguntaId, String textoPregunta, Integer edadDesde, Integer edadHasta, String respuesta,
        Repregunta repregunta) {

    /** El recuerdo que se completa y el dato por el que se preguntó. */
    public record Repregunta(String recuerdoId, DatoFaltante dato) {
    }

    /** Una respuesta del guion (no es repregunta). */
    public TrabajoDeExtraccion(Long personaId, String nombre, String apellido, LocalDate fechaNacimiento,
            int preguntaId, String textoPregunta, Integer edadDesde, Integer edadHasta, String respuesta) {
        this(personaId, nombre, apellido, fechaNacimiento, preguntaId, textoPregunta, edadDesde, edadHasta,
                respuesta, null);
    }

    @Override
    public String toString() {
        return "TrabajoDeExtraccion[persona=" + personaId + ", pregunta=" + preguntaId
                + (repregunta != null ? ", repregunta=" + repregunta.dato() : "") + "]";
    }
}
