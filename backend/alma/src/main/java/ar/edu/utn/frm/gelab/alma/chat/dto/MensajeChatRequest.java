package ar.edu.utn.frm.gelab.alma.chat.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.chat.MensajesChat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Mensaje nuevo y conversación anterior (contrato de chat, 7.2). El tope de 500 caracteres se valida en
 * {@code ChatService}, porque es una propiedad de chat.properties.
 */
public record MensajeChatRequest(
        @NotBlank(message = MensajesChat.MENSAJE_OBLIGATORIO) String mensaje,
        @Valid List<@Valid TurnoRequest> conversacion) {
}
