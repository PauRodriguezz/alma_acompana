package ar.edu.utn.frm.gelab.alma.common.exception;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;

/**
 * Usuario inexistente o contraseña incorrecta. Siempre el mismo mensaje, para no revelar si el usuario existe.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super(MensajesError.CREDENCIALES_INVALIDAS);
    }
}
