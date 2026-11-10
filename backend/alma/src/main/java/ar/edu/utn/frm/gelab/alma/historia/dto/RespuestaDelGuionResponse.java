package ar.edu.utn.frm.gelab.alma.historia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoDeRespuesta;

/**
 * Respuesta de {@code POST /guion/{preguntaId}/respuesta} y de {@code POST /repreguntas/.../respuesta} (contrato
 * de historia de vida, 7.1 y 7.2). {@code respuesta} solo viene con {@code ALARMA}: es el texto predefinido del chat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaDelGuionResponse(EstadoDeRespuesta estado, String respuesta) {

    public static RespuestaDelGuionResponse aceptada() {
        return new RespuestaDelGuionResponse(EstadoDeRespuesta.ACEPTADA, null);
    }

    public static RespuestaDelGuionResponse alarma(String texto) {
        return new RespuestaDelGuionResponse(EstadoDeRespuesta.ALARMA, texto);
    }
}
