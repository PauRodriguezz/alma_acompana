package ar.edu.utn.frm.gelab.alma.common.exception;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;

/**
 * Lo pedido no existe. La persona ve siempre el texto genérico de 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException() {
        super(MensajesError.NO_ENCONTRADO);
    }
}
