package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ExtractorDeRecuerdos;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.TrabajoDeExtraccion;
import ar.edu.utn.frm.gelab.alma.historia.service.RecuerdoService;
import ar.edu.utn.frm.gelab.alma.ia.LimitadorDePedidos;
import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import ar.edu.utn.frm.gelab.alma.ia.TurnoConversacion;
import tools.jackson.databind.json.JsonMapper;

/**
 * La cola de extracciones sin Spring ni bases: el estado "procesando" por persona, el rechazo de un encolado y
 * el apagado (contrato del grafo, 5.4 y sección 2). El modelo es manual (cada llamada espera un permiso) y
 * {@link RecuerdoService} es un doble. Ninguna espera es ilimitada: la clase tiene su {@code @Timeout} y las
 * esperas por un estado ({@link #esperarHasta}) tienen el suyo.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
class ColaDeExtraccionesTest {

    /** Cada llamada espera un permiso (interrumpible), así el test decide cuándo termina cada una. */
    static class ModeloManual implements ModeloConversacional {
        final Semaphore permisos = new Semaphore(0);
        final AtomicInteger iniciadas = new AtomicInteger();

        @Override
        public String responder(String instrucciones, List<TurnoConversacion> turnos) {
            iniciadas.incrementAndGet();
            try {
                permisos.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ModeloNoDisponibleException("Interrumpido");
            }
            return ModeloDePrueba.JSON_GENERICO;
        }
    }

    private RecuerdoService recuerdos;
    private ColaDeExtracciones cola;

    @BeforeEach
    void preparar() {
        recuerdos = mock(RecuerdoService.class);
    }

    @AfterEach
    void apagar() {
        if (cola != null) {
            cola.apagar();
        }
    }

    private ColaDeExtracciones crear(ModeloConversacional modelo) {
        ExtractorDeRecuerdos extractor = new ExtractorDeRecuerdos(modelo, new ConfiguracionHistoria(),
                Clock.systemDefaultZone(), JsonMapper.builder().build());
        cola = new ColaDeExtracciones(extractor, recuerdos);
        return cola;
    }

    private static TrabajoDeExtraccion trabajo(long personaId) {
        return new TrabajoDeExtraccion(personaId, "Ana", "Pérez", LocalDate.of(1950, 5, 12), 7,
                "¿Qué te acordás de tu escuela primaria?", 6, 12, "Una respuesta.");
    }

    // ---------- Estado "procesando" ----------

    @Test
    void laPersonaEstaProcesandoDesdeQueSeEncolaHastaQueTermina() {
        ModeloManual modelo = new ModeloManual();
        ColaDeExtracciones cola = crear(modelo);
        assertThat(cola.estaProcesando(1L)).isFalse();

        cola.encolar(trabajo(1));
        esperarHasta(() -> modelo.iniciadas.get() == 1);

        assertThat(cola.estaProcesando(1L)).as("en curso").isTrue();
        assertThat(cola.estaProcesando(2L)).as("otra persona").isFalse();

        modelo.permisos.release();
        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();

        assertThat(cola.estaProcesando(1L)).isFalse();
        verify(recuerdos, times(1)).guardar(any(), any());
    }

    @Test
    void conDosTrabajosDeLaMismaPersonaSigueProcesandoHastaQueTerminaElSegundo() {
        ModeloManual modelo = new ModeloManual();
        ColaDeExtracciones cola = crear(modelo);

        cola.encolar(trabajo(1));
        cola.encolar(trabajo(1));
        cola.encolar(trabajo(2));
        esperarHasta(() -> modelo.iniciadas.get() == 1);
        assertThat(cola.estaProcesando(1L)).isTrue();
        assertThat(cola.estaProcesando(2L)).as("en la cola, todavía sin empezar").isTrue();

        // Termina el primero de la persona 1: el segundo ya empezó y la sigue marcando.
        modelo.permisos.release();
        esperarHasta(() -> modelo.iniciadas.get() == 2);
        assertThat(cola.estaProcesando(1L)).as("queda un trabajo de la persona 1").isTrue();

        // Termina el segundo: la persona 1 deja de estar procesando; la 2 todavía no.
        modelo.permisos.release();
        esperarHasta(() -> modelo.iniciadas.get() == 3);
        esperarHasta(() -> !cola.estaProcesando(1L));
        assertThat(cola.estaProcesando(2L)).isTrue();

        modelo.permisos.release();
        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        assertThat(cola.estaProcesando(1L)).isFalse();
        assertThat(cola.estaProcesando(2L)).isFalse();
    }

    @Test
    void unTrabajoQueFallaAlGuardarNoDejaALaPersonaProcesando() {
        ModeloManual modelo = new ModeloManual();
        modelo.permisos.release(10);
        doThrow(new IllegalStateException("falló el grafo")).when(recuerdos).guardar(any(), any());
        ColaDeExtracciones cola = crear(modelo);

        cola.encolar(trabajo(1));

        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        assertThat(cola.estaProcesando(1L)).isFalse();
        verify(recuerdos).guardar(any(), any());
    }

    @Test
    void siElModeloFallaSeGuardaElRespaldoYLaPersonaDejaDeEstarProcesando() {
        ColaDeExtracciones cola = crear((instrucciones, turnos) -> {
            throw new ModeloNoDisponibleException("sin modelo");
        });

        cola.encolar(trabajo(1));

        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        assertThat(cola.estaProcesando(1L)).isFalse();
        verify(recuerdos).guardar(any(), any());
    }

    // ---------- Encolado rechazado ----------

    @Test
    void unEncoladoRechazadoNoDejaALaPersonaProcesandoParaSiempre() {
        ModeloManual modelo = new ModeloManual();
        ColaDeExtracciones cola = crear(modelo);
        cola.apagar();

        cola.encolar(trabajo(1));

        assertThat(cola.estaProcesando(1L)).as("el trabajo no fue aceptado").isFalse();
        assertThat(cola.esperarQueSeVacie(Duration.ofMillis(200))).as("no quedó nada pendiente").isTrue();
        assertThat(modelo.iniciadas.get()).isZero();
        verify(recuerdos, never()).guardar(any(), any());
    }

    // ---------- Apagado ----------

    @Test
    void elApagadoInterrumpeLaEsperaPorElTurnoYDescartaLosQueNoEmpezaron() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        AtomicInteger llamadas = new AtomicInteger();
        // El modelo real pasa por el limitador en segundo plano; acá se imita con el mismo limitador.
        ColaDeExtracciones cola = crear((instrucciones, turnos) -> {
            llamadas.incrementAndGet();
            return limitador.ejecutarEnSegundoPlano(() -> ModeloDePrueba.JSON_GENERICO);
        });
        CountDownLatch chatEmpezo = new CountDownLatch(1);
        CountDownLatch soltarChat = new CountDownLatch(1);
        ExecutorService hilos = Executors.newSingleThreadExecutor();
        try {
            // Un chat tiene el turno y no lo suelta: la extracción se queda esperando.
            hilos.submit(() -> limitador.ejecutar(() -> {
                chatEmpezo.countDown();
                try {
                    soltarChat.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }));
            assertThat(chatEmpezo.await(5, TimeUnit.SECONDS)).isTrue();
            cola.encolar(trabajo(1));
            cola.encolar(trabajo(2));
            esperarHasta(() -> llamadas.get() == 1);
            assertThat(cola.estaProcesando(1L)).isTrue();
            assertThat(cola.estaProcesando(2L)).isTrue();

            long inicio = System.nanoTime();
            cola.apagar();
            long ms = (System.nanoTime() - inicio) / 1_000_000;

            assertThat(ms).as("el apagado cortó la espera, no esperó al chat").isLessThan(4000);
            assertThat(cola.estaProcesando(1L)).as("la que esperaba turno").isFalse();
            assertThat(cola.estaProcesando(2L)).as("la que no había empezado").isFalse();
            assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(1))).isTrue();
            assertThat(llamadas.get()).as("la segunda nunca empezó").isEqualTo(1);
            verify(recuerdos, never()).guardar(any(), any());
        } finally {
            soltarChat.countDown();
            hilos.shutdownNow();
        }
    }

    // ---------- Repreguntas (5.7) ----------

    private static TrabajoDeExtraccion repregunta(long personaId) {
        return new TrabajoDeExtraccion(personaId, "Ana", "Pérez", LocalDate.of(1950, 5, 12), 7,
                "¿Dónde pasó esto?", null, null, "En Allen",
                new TrabajoDeExtraccion.Repregunta("recuerdo-1", DatoFaltante.LUGARES));
    }

    @Test
    void unaRepreguntaCompletaElRecuerdoEnLugarDeCrearUnoNuevo() {
        ModeloManual modelo = new ModeloManual();
        modelo.permisos.release(5);
        ColaDeExtracciones cola = crear(modelo);

        cola.encolar(repregunta(1));

        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        verify(recuerdos).completar(any(), any());
        verify(recuerdos, never()).guardar(any(), any());
        assertThat(cola.estaProcesando(1L)).isFalse();
    }

    @Test
    void siElModeloFallaEnUnaRepreguntaElRecuerdoNoCambia() {
        ColaDeExtracciones cola = crear((instrucciones, turnos) -> {
            throw new ModeloNoDisponibleException("sin modelo");
        });

        cola.encolar(repregunta(1));

        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        // Una repregunta no tiene respaldo: no se crea un recuerdo ni se completa nada.
        verify(recuerdos, never()).guardar(any(), any());
        verify(recuerdos, never()).completar(any(), any());
        assertThat(cola.estaProcesando(1L)).isFalse();
    }

    @Test
    void siFallaAlCompletarLaPersonaDejaDeEstarProcesando() {
        ModeloManual modelo = new ModeloManual();
        modelo.permisos.release(5);
        doThrow(new IllegalStateException("falló el grafo")).when(recuerdos).completar(any(), any());
        ColaDeExtracciones cola = crear(modelo);

        cola.encolar(repregunta(1));

        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(5))).isTrue();
        assertThat(cola.estaProcesando(1L)).isFalse();
    }

    @Test
    void apagarDosVecesOSinTrabajosNoFalla() {
        ColaDeExtracciones cola = crear(new ModeloManual());

        cola.apagar();
        cola.apagar();

        assertThat(cola.esperarQueSeVacie(Duration.ofMillis(100))).isTrue();
    }

    /** Espera (con tope de 5 s) a que se cumpla una condición; falla si no se cumple a tiempo. */
    private static void esperarHasta(BooleanSupplier condicion) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condicion.getAsBoolean()) {
            if (System.nanoTime() > limite) {
                throw new AssertionError("La condición no se cumplió en 5 s");
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrumpido", e);
            }
        }
    }
}
