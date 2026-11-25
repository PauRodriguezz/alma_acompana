package ar.edu.utn.frm.gelab.alma.auth.dto;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import jakarta.validation.constraints.NotBlank;

/**
 * Datos del formulario de ingreso. mantenerSesion es opcional y por defecto es true (CU-14).
 */
public record LoginRequest(
        @NotBlank(message = MensajesError.INGRESA_USUARIO) String nombreUsuario,
        @NotBlank(message = MensajesError.INGRESA_CONTRASENIA) String contrasenia,
        Boolean mantenerSesion) {

    public LoginRequest {
        if (mantenerSesion == null) {
            mantenerSesion = true;
        }
    }
}
