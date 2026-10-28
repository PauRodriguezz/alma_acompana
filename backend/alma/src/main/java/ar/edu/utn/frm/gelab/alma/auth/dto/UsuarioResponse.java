package ar.edu.utn.frm.gelab.alma.auth.dto;

import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.persona.dto.PreferenciaAccesibilidadDto;

/**
 * Usuario que devuelven el login y /me. id es el de la cuenta.
 * preferenciasAccesibilidad viene solo para PERSONA_MAYOR; para ADMINISTRADOR es null.
 */
public record UsuarioResponse(
        Long id,
        String nombreUsuario,
        String nombre,
        String apellido,
        Rol rol,
        PreferenciaAccesibilidadDto preferenciasAccesibilidad) {
}
