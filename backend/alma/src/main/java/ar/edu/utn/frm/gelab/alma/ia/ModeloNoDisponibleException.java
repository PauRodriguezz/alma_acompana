package ar.edu.utn.frm.gelab.alma.ia;

/**
 * El modelo no pudo responder: error, demora excesiva, respuesta vacía, sin créditos (402), sin turno o sin
 * internet (contrato de chat, 5.7). El mensaje es técnico y nunca llega a la persona ni lleva texto de la
 * conversación.
 */
public class ModeloNoDisponibleException extends RuntimeException {

    public ModeloNoDisponibleException(String detalleTecnico) {
        super(detalleTecnico);
    }

    public ModeloNoDisponibleException(String detalleTecnico, Throwable causa) {
        super(detalleTecnico, causa);
    }
}
