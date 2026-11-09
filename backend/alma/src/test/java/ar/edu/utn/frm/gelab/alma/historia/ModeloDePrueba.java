package ar.edu.utn.frm.gelab.alma.historia;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import ar.edu.utn.frm.gelab.alma.ia.TurnoConversacion;

/**
 * "Modelo de prueba" del contrato del grafo (sección 11): reemplaza la conexión con Ollama en los tests.
 * Responde lo que se le programó, en orden (un JSON válido, uno roto, un error, una demora) y, cuando no queda
 * nada programado, un JSON válido genérico. Anota cada pedido y cuántos corrieron a la vez.
 */
class ModeloDePrueba implements ModeloConversacional {

    static final String JSON_GENERICO = """
            {"titulo":"Un recuerdo","descripcion":"Contó algo de su vida.","anio":null,"anioAproximado":false,\
            "edadMencionada":null,"personas":[],"lugares":[]}""";

    /** Lo que recibió una llamada. */
    record Pedido(String instrucciones, List<TurnoConversacion> turnos, OpcionesDeLlamada opciones) {
        String mensaje() {
            return turnos.get(turnos.size() - 1).texto();
        }
    }

    /** Un paso del guion: lo que responde, cuánto tarda y, si corresponde, el error. */
    private record Paso(String salida, RuntimeException error, long demoraMs) {
    }

    private final ConcurrentLinkedDeque<Paso> guion = new ConcurrentLinkedDeque<>();
    private final List<Pedido> pedidos = new CopyOnWriteArrayList<>();
    private final AtomicInteger enCurso = new AtomicInteger();
    private volatile int maximoSimultaneo;

    /** Programa la próxima respuesta. */
    ModeloDePrueba responde(String salida) {
        guion.add(new Paso(salida, null, 0));
        return this;
    }

    /** Programa una respuesta que tarda. */
    ModeloDePrueba respondeConDemora(String salida, long demoraMs) {
        guion.add(new Paso(salida, null, demoraMs));
        return this;
    }

    /** Programa un fallo del modelo (error, demora excesiva, sin turno). */
    ModeloDePrueba falla() {
        guion.add(new Paso(null, new ModeloNoDisponibleException("Falla de prueba"), 0));
        return this;
    }

    void reiniciar() {
        guion.clear();
        pedidos.clear();
        maximoSimultaneo = 0;
    }

    List<Pedido> pedidos() {
        return pedidos;
    }

    int maximoSimultaneo() {
        return maximoSimultaneo;
    }

    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos) {
        return atender(new Pedido(instrucciones, List.copyOf(turnos), null));
    }

    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos, OpcionesDeLlamada opciones) {
        return atender(new Pedido(instrucciones, List.copyOf(turnos), opciones));
    }

    private String atender(Pedido pedido) {
        pedidos.add(pedido);
        int simultaneos = enCurso.incrementAndGet();
        maximoSimultaneo = Math.max(maximoSimultaneo, simultaneos);
        try {
            Paso paso = guion.pollFirst();
            if (paso == null) {
                return JSON_GENERICO;
            }
            if (paso.demoraMs() > 0) {
                try {
                    Thread.sleep(paso.demoraMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (paso.error() != null) {
                throw paso.error();
            }
            return paso.salida();
        } finally {
            enCurso.decrementAndGet();
        }
    }
}
