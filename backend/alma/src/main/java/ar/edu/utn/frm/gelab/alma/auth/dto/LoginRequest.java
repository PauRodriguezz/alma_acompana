package ar.edu.utn.frm.gelab.alma.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos del formulario de ingreso. mantenerSesion es opcional y por defecto es true (CU-14).
 */
public record LoginRequest(
        @NotBlank(message = "Ingresá tu nombre de usuario.") String nombreUsuario,
        @NotBlank(message = "Ingresá tu contraseña.") String contrasenia,
        Boolean mantenerSesion) {

    public LoginRequest {
        if (mantenerSesion == null) {
            mantenerSesion = true;
        }
    }
}
