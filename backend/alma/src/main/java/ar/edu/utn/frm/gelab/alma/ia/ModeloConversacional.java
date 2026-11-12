package ar.edu.utn.frm.gelab.alma.ia;

import java.util.List;

/**
 * Lo único que el módulo de chat conoce del modelo de IA (arquitectura, sección 4). Solo {@code ia/}
 * importa Spring AI.
 */
public interface ModeloConversacional {

    /**
     * @param instrucciones el system prompt ya armado
     * @param turnos        la conversación en orden, del más viejo al más nuevo; el último es el mensaje nuevo
     *                      de la persona
     * @return el texto final de la respuesta, sin razonamiento y sin espacios en los extremos
     * @throws ModeloNoDisponibleException si el modelo falla por cualquier motivo
     */
    String responder(String instrucciones, List<TurnoConversacion> turnos);

    /**
     * Igual que {@link #responder(String, List)} pero con parámetros propios de esta llamada y el tope de tiempo
     * del cliente de extracción (contrato del grafo, 5.4). La implementación por defecto ignora las opciones y
     * delega en el método de arriba: un modelo que no las conozca (por ejemplo, uno de prueba) sigue sirviendo.
     */
    default String responder(String instrucciones, List<TurnoConversacion> turnos, OpcionesDeLlamada opciones) {
        return responder(instrucciones, turnos);
    }

    /** Parámetros de una sola llamada. El chat no los usa: conserva sus valores. */
    record OpcionesDeLlamada(double temperatura, int maxTokens) {
    }
}
