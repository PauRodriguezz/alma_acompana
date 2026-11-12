package ar.edu.utn.frm.gelab.alma.historia.domain;

/** Qué responde {@code GET /api/historia/repreguntas/siguiente} (contrato del grafo, 7.5). */
public enum EstadoRepregunta {
    /** Hay una extracción en cola o en curso de la persona: conviene volver a preguntar en unos segundos. */
    PROCESANDO,
    /** Hay una repregunta candidata. */
    HAY,
    /** No hay ninguna repregunta candidata. */
    NINGUNA
}
