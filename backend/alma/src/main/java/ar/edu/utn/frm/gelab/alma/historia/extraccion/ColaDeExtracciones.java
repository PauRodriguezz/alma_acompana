package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Cada trabajo llama al extractor (sin transacción de Neo4j) y después escribe el recuerdo en una
 * transacción. Dentro del extractor, la llamada al modelo espera su turno detrás de los chats y sin tope de
 * espera ({@code LimitadorDePedidos.ejecutarEnSegundoPlano}). Un trabajo nunca lanza errores hacia afuera: lo
 * que falle se registra, sin texto de la persona (5.9).
 *
 * <p><b>Estado "procesando":</b> {@link #estaProcesando(Long)} es verdadero mientras la persona tenga un
 * trabajo en la cola o en curso. Lo va a leer {@code GET /api/historia/repreguntas/siguiente} (7.5).
 *
 * <p><b>Apagado:</b> {@link #apagar()} interrumpe el trabajo que esté esperando turno o llamando al modelo y
 * descarta los que no empezaron; ninguno de los dos deja a nadie como "procesando".
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

    /** Protege los contadores y sirve de señal para {@link #esperarQueSeVacie}. */
    private final Object cerrojo = new Object();
    private final Map<Long, Integer> pendientesPorPersona = new HashMap<>();
    private int pendientes;

    public ColaDeExtracciones(ExtractorDeRecuerdos extractor, RecuerdoService recuerdos) {
        this.extractor = extractor;
        this.recuerdos = recuerdos;
    }

    /**
     * Pone el trabajo al final de la cola y vuelve enseguida. No lanza errores. La persona queda como
     * "procesando" solo si el trabajo fue aceptado: si la cola ya está apagada, el contador vuelve atrás.
     */
    public void encolar(TrabajoDeExtraccion trabajo) {
        // Se sube antes de entregar el trabajo: si no, un trabajo muy rápido podría terminar (y restar) antes.
        sumar(trabajo.personaId());
        try {
            hilo.execute(new Tarea(trabajo));
        } catch (RuntimeException e) {
            restar(trabajo.personaId());
            log.warn("No se pudo encolar una extracción ({})", e.getClass().getSimpleName());
        }
    }

    /** ¿La persona tiene una extracción en la cola o en curso? */
    public boolean estaProcesando(Long personaId) {
        synchronized (cerrojo) {
            return pendientesPorPersona.containsKey(personaId);
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

    /**
     * Apaga la cola: interrumpe el trabajo en curso (si espera turno o llama al modelo, se corta; no se escribe
     * su recuerdo) y descarta los que no empezaron, bajando sus contadores. Espera hasta 5 s a que el hilo
     * termine. Después, {@link #encolar} rechaza todo.
     */
    @PreDestroy
    public void apagar() {
        List<Runnable> sinCorrer = hilo.shutdownNow();
        for (Runnable tarea : sinCorrer) {
            if (tarea instanceof Tarea descartada) {
                restar(descartada.trabajo().personaId());
            }
        }
        if (!sinCorrer.isEmpty()) {
            log.info("Apagado: se descartaron {} extracciones que no habían empezado", sinCorrer.size());
        }
        try {
            if (!hilo.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("El hilo de extracciones no terminó a tiempo en el apagado");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void procesar(TrabajoDeExtraccion trabajo) {
        try {
            ResultadoDeExtraccion resultado = extractor.extraer(trabajo);
            if (Thread.currentThread().isInterrupted()) {
                // Apagado: la espera o la llamada se cortaron, no se escribe nada.
                log.info("Extracción interrumpida por el apagado: no se guarda el recuerdo de la persona {}",
                        trabajo.personaId());
                return;
            }
            if (trabajo.repregunta() == null) {
                recuerdos.guardar(trabajo, resultado.respuesta());
            } else if (resultado.respaldo()) {
                // Una repregunta no tiene respaldo (5.4): el recuerdo ya existe y simplemente no cambia.
                log.info("La repregunta de la persona {} no se pudo extraer: el recuerdo no cambia",
                        trabajo.personaId());
            } else {
                recuerdos.completar(trabajo, resultado.respuesta());
            }
        } catch (RuntimeException e) {
            // Contrato, 5.4: el trabajo termina sin lanzar errores. No se registra el mensaje: podría citar texto.
            log.error("No se pudo guardar el recuerdo de la persona {} ({})", trabajo.personaId(),
                    e.getClass().getSimpleName());
        } finally {
            restar(trabajo.personaId());
        }
    }

    private void sumar(Long personaId) {
        synchronized (cerrojo) {
            pendientes++;
            pendientesPorPersona.merge(personaId, 1, Integer::sum);
        }
    }

    private void restar(Long personaId) {
        synchronized (cerrojo) {
            pendientes--;
            pendientesPorPersona.computeIfPresent(personaId, (id, cuantos) -> cuantos > 1 ? cuantos - 1 : null);
            cerrojo.notifyAll();
        }
    }

    /** El trabajo con su persona, para poder bajar el contador si se descarta sin correr. */
    private final class Tarea implements Runnable {

        private final TrabajoDeExtraccion trabajo;

        private Tarea(TrabajoDeExtraccion trabajo) {
            this.trabajo = trabajo;
        }

        TrabajoDeExtraccion trabajo() {
            return trabajo;
        }

        @Override
        public void run() {
            procesar(trabajo);
        }
    }
}
