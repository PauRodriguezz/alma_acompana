package ar.edu.utn.frm.gelab.alma.historia.dto;

/**
 * La próxima pregunta pendiente con su introducción (contrato del grafo, 7.1). Si no queda ninguna,
 * {@code pregunta} e {@code introduccion} son nulas y {@code mensajeFinal} trae el texto de cierre.
 */
public record ProximaPreguntaResponse(PreguntaDto pregunta, String introduccion, String mensajeFinal,
        boolean quedanPendientes) {
}
