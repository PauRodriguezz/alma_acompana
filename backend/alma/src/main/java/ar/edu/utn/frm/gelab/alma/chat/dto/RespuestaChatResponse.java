package ar.edu.utn.frm.gelab.alma.chat.dto;

/**
 * Respuesta completa de Alma (contrato de chat, 7.2). Tiene la misma forma si la generó el modelo o es una
 * respuesta predefinida. {@code alarma} es {@code true} cuando respondió el detector de riesgo (malestar grave o
 * urgencia médica) y {@code false} cuando generó la respuesta el modelo.
 */
public record RespuestaChatResponse(String respuesta, boolean alarma) {
}
