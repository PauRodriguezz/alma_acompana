package ar.edu.utn.frm.gelab.alma.ia;

/**
 * El modelo no pudo responder: error, demora excesiva, respuesta vacía, sin créditos (402), sin turno o sin
 * internet (contrato de chat, 5.7). El mensaje es técnico y nunca llega a la persona ni lleva texto de la
 * conversación.
 *
 * <p>{@link #esTransitorio()} es {@code true} cuando el proveedor falló por algo que suele pasar solo, como un 503
 * (ver {@link ErroresTransitorios}). Lo usa la extracción de recuerdos para reintentar una vez (contrato del grafo,
 * 5.4); el chat no lo mira. Un corte por tiempo, una clave faltante o un 400 no son transitorios.
 */
public class ModeloNoDisponibleException extends RuntimeException {

    private final boolean transitorio;

    public ModeloNoDisponibleException(String detalleTecnico) {
        super(detalleTecnico);
        this.transitorio = false;
    }

    public ModeloNoDisponibleException(String detalleTecnico, Throwable causa) {
        this(detalleTecnico, causa, false);
    }

    public ModeloNoDisponibleException(String detalleTecnico, Throwable causa, boolean transitorio) {
        super(detalleTecnico, causa);
        this.transitorio = transitorio;
    }

    /** ¿Es un error del proveedor que suele pasar solo (por ejemplo, un 503)? */
    public boolean esTransitorio() {
        return transitorio;
    }
}
