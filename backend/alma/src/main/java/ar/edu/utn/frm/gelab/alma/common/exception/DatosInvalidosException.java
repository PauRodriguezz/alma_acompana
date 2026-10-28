package ar.edu.utn.frm.gelab.alma.common.exception;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;
import ar.edu.utn.frm.gelab.alma.common.MensajesError;

/**
 * Una regla de validación que Bean Validation no puede expresar. El handler global la convierte en el
 * mismo 400 con la lista "errores" que produce Bean Validation.
 */
public class DatosInvalidosException extends RuntimeException {

    private final List<ErrorCampo> errores;

    public DatosInvalidosException(List<ErrorCampo> errores) {
        super(MensajesError.DATOS_INVALIDOS);
        this.errores = List.copyOf(errores);
    }

    public DatosInvalidosException(String campo, String mensaje) {
        this(List.of(new ErrorCampo(campo, mensaje)));
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
