package ar.edu.utn.frm.gelab.alma.historia.domain;

/** Resultado de responder una pregunta o una repregunta (contrato de historia de vida, 7.1 y 7.2). */
public enum EstadoDeRespuesta {
    /** La respuesta se guardó y la extracción quedó encolada (202). */
    ACEPTADA,
    /** Saltó el detector de riesgo: no se guardó ni se encoló nada (200). */
    ALARMA
}
