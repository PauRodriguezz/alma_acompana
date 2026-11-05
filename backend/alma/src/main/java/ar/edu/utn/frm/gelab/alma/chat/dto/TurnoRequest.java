package ar.edu.utn.frm.gelab.alma.chat.dto;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Un turno anterior de la conversación (contrato de chat, 5.9). El largo del texto no se rechaza: el
 * servicio lo corta.
 */
public record TurnoRequest(
        @NotNull(message = MensajesError.DATOS_INVALIDOS) RolChat rol,
        @NotBlank(message = MensajesError.DATOS_INVALIDOS) String texto) {
}
