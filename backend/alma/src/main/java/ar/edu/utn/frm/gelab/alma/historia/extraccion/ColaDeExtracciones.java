package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.historia.service.RecuerdoService;
import jakarta.annotation.PreDestroy;

/**
 * Cola de extracciones (contrato del grafo, 5.4 y sección 2): <b>en memoria</b> y con <b>un solo hilo</b>, así
 * que los trabajos se atienden de a uno y en orden de llegada. Si el back se reinicia, se pierden los que
 * esperaban (límite aceptado).
 *
 * <p>Cada trabajo llama al extractor (sin transacción) y después escribe el recuerdo en una transacción de
 * Neo4j. Un trabajo nunca lanza errores hacia afuera: lo que falle se registra, sin texto de la persona (5.9).
 *
 * <p>Versión mínima del paso 4. El paso 5 suma el estado "procesando" por persona y la prioridad del chat.
 */
@Component
public class ColaDeExtracciones {

    private static final Logger log = LoggerFactory.getLogger(ColaDeExtracciones.class);

    private final ExtractorDeRecuerdos extractor;
    private final RecuerdoService recuerdos;
    private final ExecutorService hilo = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "extraccion-de-recuerdos");
        hilo.setDaemon(true);
        return hilo;
    });
    private final Object cerrojo = new Object();
    private int pendientes;

    public ColaDeExtracciones(ExtractorDeRecuerdos extractor, RecuerdoService recuerdos) {
        this.extractor = extractor;
        this.recuerdos = recuerdos;
    }

    /** Pone el trabajo al final de la cola y vuelve enseguida. No lanza errores. */
    public void encolar(TrabajoDeExtraccion trabajo) {
        synchronized (cerrojo) {
            pendientes++;
        }
        try {
            hilo.execute(() -> procesar(trabajo));
        } catch (RuntimeException e) {
            // La cola ya estaba cerrada (apagado del back).
            terminar();
            log.warn("No se pudo encolar una extracción ({})", e.getClass().getSimpleName());
        }
    }

    /**
     * Espera a que no quede ningún trabajo encolado ni en curso. Pensado para las pruebas.
     *
     * @return {@code true} si la cola quedó vacía; {@code false} si se acabó el tiempo
     */
    public boolean esperarQueSeVacie(Duration maximo) {
        long limite = System.nanoTime() + maximo.toNanos();
        synchronized (cerrojo) {
            while (pendientes > 0) {
                long faltanteMs = TimeUnit.NANOSECONDS.toMillis(limite - System.nanoTime());
                if (faltanteMs <= 0) {
                    return false;
                }
                try {
                    cerrojo.wait(faltanteMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    private void procesar(TrabajoDeExtraccion trabajo) {
        try {
            ResultadoDeExtraccion resultado = extractor.extraer(trabajo);
            recuerdos.guardar(trabajo, resultado.respuesta());
        } catch (RuntimeException e) {
            // Contrato, 5.4: el trabajo termina sin lanzar errores. No se registra el mensaje: podría citar texto.
            log.error("No se pudo guardar el recuerdo de la persona {} ({})", trabajo.personaId(),
                    e.getClass().getSimpleName());
        } finally {
            terminar();
        }
    }

    private void terminar() {
        synchronized (cerrojo) {
            pendientes--;
            cerrojo.notifyAll();
        }
    }

    @PreDestroy
    void apagar() {
        hilo.shutdown();
        try {
            if (!hilo.awaitTermination(5, TimeUnit.SECONDS)) {
                hilo.shutdownNow();
            }
        } catch (InterruptedException e) {
            hilo.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
