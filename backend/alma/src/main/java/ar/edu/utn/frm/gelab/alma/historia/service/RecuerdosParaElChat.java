package ar.edu.utn.frm.gelab.alma.historia.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.chat.MensajesChat;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.repository.EstadoDePregunta;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.NombreConocido;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaElPrompt;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import jakarta.annotation.PreDestroy;

/**
 * Lo único de historia que usa el chat (contrato de historia de vida, 2.10, 2.11, 5.5 y 10.3): devuelve el texto de
 * {@code {progreso}} y de {@code {recuerdos}} ya armado. El chat no lee Neo4j. Sin llamadas al modelo: se compara el
 * mensaje normalizado con los nombres del grafo y con las frases de etapa. Solo lee, y nunca crea el nodo de la
 * persona. Una falla al leer el grafo no rompe el chat: la parte que falló queda en "No hay información." y el
 * log lleva solo el tipo de error (nunca el mensaje ni los recuerdos).
 *
 * <p><b>Presupuesto de tiempo propio.</b> Toda la lectura del grafo (progreso y recuerdos juntos) corre en un
 * ejecutor chico y acotado, con un tope de {@code alma.historia.lectura-chat-timeout-ms} (2 s por defecto). Si se
 * pasa del tope, o si todos los hilos están ocupados (se rechaza de inmediato, sin cola), el chat sigue con "No hay
 * información." y no espera más: con Neo4j caído o con una red que descarta paquetes, el driver tardaría hasta
 * 21 s por consulta. No cambia la configuración del driver, así que las escrituras no se ven afectadas. La consulta
 * que se pasó del tope sigue en su hilo hasta que el driver la corta (se la interrumpe, pero un socket bloqueado no
 * siempre responde): por eso el ejecutor tiene un máximo de hilos.
 */
@Service
public class RecuerdosParaElChat {

    private static final Logger log = LoggerFactory.getLogger(RecuerdosParaElChat.class);

    /** Los dos textos listos para el prompt. */
    public record Resultado(String progreso, String recuerdos) {

        static Resultado sinInformacion() {
            return new Resultado(MensajesChat.SIN_INFORMACION, MensajesChat.SIN_INFORMACION);
        }
    }

    private final PersonaMayorRepository personaMayorRepository;
    private final LecturaDelGrafo lectura;
    private final ConfiguracionHistoria configuracion;
    private final ThreadPoolExecutor ejecutor;

    public RecuerdosParaElChat(PersonaMayorRepository personaMayorRepository, LecturaDelGrafo lectura,
            ConfiguracionHistoria configuracion) {
        this.personaMayorRepository = personaMayorRepository;
        this.lectura = lectura;
        this.configuracion = configuracion;
        // Sin cola: con todos los hilos ocupados, submit lanza RejectedExecutionException al instante.
        AtomicInteger numero = new AtomicInteger();
        this.ejecutor = new ThreadPoolExecutor(0, Math.max(1, configuracion.getLecturaChatHilosMax()), 30,
                TimeUnit.SECONDS, new SynchronousQueue<>(), tarea -> {
                    Thread hilo = new Thread(tarea, "alma-recuerdos-chat-" + numero.incrementAndGet());
                    hilo.setDaemon(true);
                    return hilo;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdownNow();
    }

    /**
     * El progreso y hasta 3 recuerdos relacionados con el mensaje actual de la cuenta. Sin persona mayor, sin
     * nodo en el grafo, sin datos, ante cualquier falla, si se pasa del tope de tiempo o si el ejecutor está lleno,
     * "No hay información." (la parte que corresponda). Nunca espera más que el tope.
     */
    public Resultado armar(Long cuentaId, String mensaje) {
        Long personaId;
        try {
            personaId = personaMayorRepository.findByCuentaId(cuentaId).map(p -> p.getId()).orElse(null);
        } catch (RuntimeException e) {
            return fallo("la persona", e);
        }
        if (personaId == null) {
            return Resultado.sinInformacion();
        }
        return conTope(personaId, mensaje);
    }

    /** Lee el grafo en el ejecutor y espera como máximo el tope; si no llega a tiempo, sin información. */
    private Resultado conTope(Long personaId, String mensaje) {
        // Bandera propia de esta lectura: se pone ANTES de cancelar. No alcanza con la marca de interrupción del
        // hilo, porque el driver o Spring Data pueden limpiarla antes de que la lectura llegue a su catch.
        AtomicBoolean cancelada = new AtomicBoolean();
        Future<Resultado> trabajo;
        try {
            trabajo = ejecutor.submit(() -> {
                String progreso = progreso(personaId, cancelada);
                // Si el tope ya canceló la lectura, no se hacen más consultas (el resultado ya no lo lee nadie).
                if (interrumpida(cancelada)) {
                    return Resultado.sinInformacion();
                }
                return new Resultado(progreso, recuerdos(personaId, mensaje, cancelada));
            });
        } catch (RejectedExecutionException e) {
            log.warn("Lectura de historia para el chat rechazada: todos los hilos están ocupados");
            return Resultado.sinInformacion();
        }
        try {
            return trabajo.get(configuracion.getLecturaChatTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cancelada.set(true);
            trabajo.cancel(true);
            log.warn("La lectura de historia para el chat superó el tope de {} ms",
                    configuracion.getLecturaChatTimeoutMs());
            return Resultado.sinInformacion();
        } catch (InterruptedException e) {
            cancelada.set(true);
            trabajo.cancel(true);
            Thread.currentThread().interrupt();
            return Resultado.sinInformacion();
        } catch (ExecutionException e) {
            // progreso y recuerdos atrapan sus RuntimeException: acá solo llegaría un Error.
            log.error("La lectura de historia para el chat falló ({})", e.getCause().getClass().getSimpleName());
            return Resultado.sinInformacion();
        }
    }

    /** 5.5: solo cantidades, de la misma lectura de solo lectura que {@code estado} (P4), sin {@code MERGE}. */
    private String progreso(Long personaId, AtomicBoolean cancelada) {
        try {
            List<EstadoDePregunta> todas = lectura.estadoDeTodas(personaId);
            int respondidas = (int) todas.stream().filter(e -> "RESPONDIO".equals(e.tipo())).count();
            int salteadas = (int) todas.stream().filter(e -> "SALTEO".equals(e.tipo())).count();
            if (respondidas + salteadas == 0) {
                return MensajesChat.SIN_INFORMACION;
            }
            return TextoDeRecuerdos.progreso(respondidas, salteadas, todas.size() - respondidas - salteadas);
        } catch (RuntimeException e) {
            if (interrumpida(cancelada)) {
                registrarCancelada(e);
            } else {
                log.error("No se pudo leer el progreso para el chat ({})", e.getClass().getSimpleName());
            }
            return MensajesChat.SIN_INFORMACION;
        }
    }

    private String recuerdos(Long personaId, String mensaje, AtomicBoolean cancelada) {
        try {
            String normalizado = DetectorDeRiesgo.normalizar(mensaje);
            if (normalizado.isEmpty()) {
                return MensajesChat.SIN_INFORMACION;
            }
            // Primero los de persona o lugar, después los de etapa; cada grupo, de más reciente a más antiguo.
            Map<String, RecuerdoParaElPrompt> elegidos = new LinkedHashMap<>();
            agregar(elegidos, porNombre(personaId, normalizado, cancelada));
            if (interrumpida(cancelada)) {
                return MensajesChat.SIN_INFORMACION;
            }
            agregar(elegidos, porEtapa(personaId, normalizado, cancelada));
            if (elegidos.isEmpty()) {
                return MensajesChat.SIN_INFORMACION;
            }
            int descripcionMax = configuracion.getDescripcionPromptMax();
            return elegidos.values().stream().limit(configuracion.getMaxRecuerdosPrompt())
                    .map(r -> TextoDeRecuerdos.linea(r, descripcionMax))
                    .reduce((a, b) -> a + "\n" + b).orElse(MensajesChat.SIN_INFORMACION);
        } catch (RuntimeException e) {
            if (interrumpida(cancelada)) {
                registrarCancelada(e);
            } else {
                log.error("No se pudieron leer los recuerdos para el chat ({})", e.getClass().getSimpleName());
            }
            return MensajesChat.SIN_INFORMACION;
        }
    }

    /** P16 y P17 para detectar las menciones; P13 y P14 con el {@code nombreNormalizado} guardado. */
    private List<RecuerdoParaElPrompt> porNombre(Long personaId, String normalizado, AtomicBoolean cancelada) {
        List<RecuerdoParaElPrompt> encontrados = new ArrayList<>();
        if (interrumpida(cancelada)) {
            return encontrados;
        }
        for (NombreConocido persona : lectura.personasConocidas(personaId)) {
            if (TextoDeRecuerdos.menciona(normalizado, persona.nombre())) {
                if (interrumpida(cancelada)) {
                    return encontrados;
                }
                encontrados.addAll(lectura.recuerdosDePersona(personaId, persona.nombreNormalizado()));
            }
        }
        if (interrumpida(cancelada)) {
            return encontrados;
        }
        for (NombreConocido lugar : lectura.lugaresConocidos(personaId)) {
            if (TextoDeRecuerdos.menciona(normalizado, lugar.nombre())) {
                if (interrumpida(cancelada)) {
                    return encontrados;
                }
                encontrados.addAll(lectura.recuerdosDeLugar(personaId, lugar.nombreNormalizado()));
            }
        }
        return encontrados;
    }

    /** P15 por cada etapa con alguna frase en el mensaje. */
    private List<RecuerdoParaElPrompt> porEtapa(Long personaId, String normalizado, AtomicBoolean cancelada) {
        List<RecuerdoParaElPrompt> encontrados = new ArrayList<>();
        for (Map.Entry<EtapaDeVida, List<String>> etapa : configuracion.getFrasesEtapa().entrySet()) {
            if (etapa.getValue().stream().anyMatch(frase -> TextoDeRecuerdos.menciona(normalizado, frase))) {
                if (interrumpida(cancelada)) {
                    return encontrados;
                }
                encontrados.addAll(lectura.recuerdosDeEtapa(personaId, etapa.getKey().name()));
            }
        }
        return encontrados;
    }

    /** ¿El tope de tiempo canceló esta lectura? Entonces se dejan de hacer consultas (no se limpia la marca). */
    private static boolean interrumpida(AtomicBoolean cancelada) {
        return cancelada.get() || Thread.currentThread().isInterrupted();
    }

    /** Una lectura cancelada por el tope no es un error: el WARN del tope ya avisó. Solo el tipo, nunca el mensaje. */
    private static void registrarCancelada(RuntimeException e) {
        log.info("Lectura de historia para el chat cancelada por el tope; se descarta ({})",
                e.getClass().getSimpleName());
    }

    /** Suma un grupo sin repetir (el primer grupo gana) y de más reciente a más antiguo por {@code fechaRegistro}. */
    private static void agregar(Map<String, RecuerdoParaElPrompt> elegidos, List<RecuerdoParaElPrompt> grupo) {
        grupo.stream()
                .sorted(Comparator.comparing(RecuerdoParaElPrompt::fechaRegistro,
                        Comparator.nullsLast(Comparator.<ZonedDateTime>reverseOrder())))
                .forEach(r -> elegidos.putIfAbsent(r.id(), r));
    }

    private static Resultado fallo(String que, RuntimeException e) {
        log.error("No se pudo leer {} para el chat ({})", que, e.getClass().getSimpleName());
        return Resultado.sinInformacion();
    }
}
