package ar.edu.utn.frm.gelab.alma.common.exception;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;
import ar.edu.utn.frm.gelab.alma.common.MensajesError;

/**
 * 400 por una regla que Bean Validation no puede expresar. El handler global lo convierte en el mismo
 * Problem Details con la lista "errores" por campo. Con un detalle propio del caso de uso (por ejemplo
 * "Completá todos los campos requeridos…") cuando las reglas viven en el servicio; sin él, con el texto
 * general {@link MensajesError#DATOS_INVALIDOS}.
 */
public class DatosInvalidosException extends RuntimeException {

    private final transient List<ErrorCampo> errores;

    public DatosInvalidosException(String detalle, List<ErrorCampo> errores) {
        super(detalle);
        this.errores = List.copyOf(errores);
    }

    public DatosInvalidosException(List<ErrorCampo> errores) {
        this(MensajesError.DATOS_INVALIDOS, errores);
    }

    public DatosInvalidosException(String campo, String mensaje) {
        this(List.of(new ErrorCampo(campo, mensaje)));
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
