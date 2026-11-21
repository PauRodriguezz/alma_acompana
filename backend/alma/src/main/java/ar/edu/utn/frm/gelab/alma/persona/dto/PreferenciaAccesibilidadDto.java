package ar.edu.utn.frm.gelab.alma.persona.dto;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import jakarta.validation.constraints.NotNull;

/**
 * Preferencias de accesibilidad tal como viajan en la API (contrato, sección 4.3).
 */
public record PreferenciaAccesibilidadDto(
        @NotNull(message = MensajesError.ELEGI_TAMANIO_LETRA) TamanioLetra tamanioLetra,
        @NotNull(message = MensajesError.INDICA_ALTO_CONTRASTE) Boolean altoContraste) {

    public static PreferenciaAccesibilidadDto de(PreferenciaAccesibilidad preferencia) {
        return new PreferenciaAccesibilidadDto(preferencia.getTamanioLetra(), preferencia.getAltoContraste());
    }
}
