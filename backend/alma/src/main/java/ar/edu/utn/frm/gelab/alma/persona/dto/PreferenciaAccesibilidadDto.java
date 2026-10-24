package ar.edu.utn.frm.gelab.alma.persona.dto;

import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import jakarta.validation.constraints.NotNull;

/**
 * Preferencias de accesibilidad tal como viajan en la API (contrato, sección 4.3).
 */
public record PreferenciaAccesibilidadDto(
        @NotNull(message = "Elegí un tamaño de letra.") TamanioLetra tamanioLetra,
        @NotNull(message = "Indicá si querés alto contraste.") Boolean altoContraste) {

    public static PreferenciaAccesibilidadDto de(PreferenciaAccesibilidad preferencia) {
        return new PreferenciaAccesibilidadDto(preferencia.getTamanioLetra(), preferencia.getAltoContraste());
    }
}
