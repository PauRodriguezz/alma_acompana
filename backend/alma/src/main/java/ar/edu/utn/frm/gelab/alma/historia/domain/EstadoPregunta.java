package ar.edu.utn.frm.gelab.alma.historia.domain;

/** Estado de una pregunta del guion para una persona (contrato del grafo, sección 3). */
public enum EstadoPregunta {
    /** Tiene una relación {@code RESPONDIO}. */
    RESPONDIDA,
    /** Tiene una relación {@code SALTEO}. */
    SALTEADA,
    /** No tiene ninguna de las dos. */
    PENDIENTE
}
