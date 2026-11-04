package ar.edu.utn.frm.gelab.alma.bienestar.dto;

import ar.edu.utn.frm.gelab.alma.bienestar.MensajesAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.domain.NivelAnimo;
import jakarta.validation.constraints.NotNull;

/**
 * Nivel que eligió la persona (contrato de ánimo, 5.7). Sirve para registrar y para corregir.
 */
public record RegistrarAnimoRequest(
        @NotNull(message = MensajesAnimo.NIVEL_OBLIGATORIO) NivelAnimo nivelAnimo) {
}
