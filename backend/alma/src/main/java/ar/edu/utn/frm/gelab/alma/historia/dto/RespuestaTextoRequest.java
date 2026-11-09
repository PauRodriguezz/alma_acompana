package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.MensajesHistoria;
import jakarta.validation.constraints.NotBlank;

/**
 * Respuesta de texto a una pregunta del guion (contrato del grafo, 7.3) y, más adelante, a una repregunta (7.6).
 * Se llama distinto del {@code RespuestaRequest} de juegos: con el mismo nombre simple, springdoc mezcla los dos
 * esquemas en {@code /v3/api-docs}. El tope de caracteres se valida en
 * {@code HistoriaDeVidaService}, porque es una propiedad de historia.properties.
 */
public record RespuestaTextoRequest(@NotBlank(message = MensajesHistoria.RESPUESTA_OBLIGATORIA) String texto) {
}
