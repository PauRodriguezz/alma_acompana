package ar.edu.utn.frm.gelab.alma.bienestar.dto;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.bienestar.MensajesAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.domain.NivelAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.domain.RegistroAnimo;

/**
 * Registro de ánimo con la pregunta que Alma hace para ese nivel (contrato de ánimo, 7.1).
 */
public record RegistroAnimoResponse(
        Long id,
        NivelAnimo nivelAnimo,
        LocalDateTime fechaHora,
        String preguntaContextual) {

    public static RegistroAnimoResponse de(RegistroAnimo registro) {
        return new RegistroAnimoResponse(registro.getId(), registro.getNivelAnimo(), registro.getFechaHora(),
                MensajesAnimo.preguntaDe(registro.getNivelAnimo()));
    }
}
