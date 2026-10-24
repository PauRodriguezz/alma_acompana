package ar.edu.utn.frm.gelab.alma.auth.dto;

/**
 * Respuesta del login: el token, cuánto dura y el usuario.
 */
public record LoginResponse(
        String token,
        String tipoToken,
        long expiraEnSegundos,
        UsuarioResponse usuario) {

    public static final String TIPO_BEARER = "Bearer";
}
