package ar.edu.utn.frm.gelab.alma.common.exception;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;

/**
 * 400 con un detail propio del caso de uso (por ejemplo "Completá todos los campos requeridos…")
 * y la lista "errores" por campo. Se usa cuando las reglas viven en el servicio.
 */
public class DatosInvalidosException extends RuntimeException {

    private final transient List<ErrorCampo> errores;

    public DatosInvalidosException(String detalle, List<ErrorCampo> errores) {
        super(detalle);
        this.errores = List.copyOf(errores);
    }

    public List<ErrorCampo> getErrores() {
        return errores;
    }
}
