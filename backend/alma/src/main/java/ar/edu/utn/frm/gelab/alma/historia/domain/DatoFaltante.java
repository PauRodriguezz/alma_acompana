package ar.edu.utn.frm.gelab.alma.historia.domain;

/**
 * El dato de un recuerdo por el que se puede repreguntar (contrato del grafo, 5.7). El orden en que una pregunta
 * del guion los ofrece está en su propiedad {@code repreguntas}.
 */
public enum DatoFaltante {
    /** Falta el año, o es solo una estimación por la franja de edad de la pregunta. */
    ANIO,
    /** El recuerdo no tiene ningún lugar. */
    LUGARES,
    /** El recuerdo no tiene ninguna persona. */
    PERSONAS
}
