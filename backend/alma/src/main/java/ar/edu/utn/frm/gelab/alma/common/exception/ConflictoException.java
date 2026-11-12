package ar.edu.utn.frm.gelab.alma.common.exception;

/**
 * Lo pedido choca con el estado actual (por ejemplo, una pregunta que ya fue respondida). A diferencia de
 * {@link RecursoNoEncontradoException}, el texto cambia según el caso de uso: la persona ve el mensaje de
 * la excepción, en un 409 con el título general de {@code MensajesError}.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
