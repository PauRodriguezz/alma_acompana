package ar.edu.utn.frm.gelab.alma.historia.domain;

/** Cómo se obtuvo el año de un recuerdo (modelo de grafo, "Valores enumerados"). Se guarda como texto. */
public enum PrecisionAnio {
    /** La persona dijo el año. */
    EXACTA,
    /** La persona dijo un año aproximado. */
    APROXIMADA,
    /** Calculado con la edad mencionada y la fecha de nacimiento. */
    CALCULADA_POR_EDAD,
    /** Estimado con la franja de edad de la pregunta. */
    ESTIMADA
}
