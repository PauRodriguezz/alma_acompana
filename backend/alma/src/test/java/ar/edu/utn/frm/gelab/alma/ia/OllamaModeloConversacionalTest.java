package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.sun.net.httpserver.HttpServer;

/**
 * {@link OllamaModeloConversacional} contra un servidor falso que imita a Ollama: el pedido que sale
 * (razonamiento desactivado, parámetros), la limpieza, los errores, el corte por tiempo y la ausencia de
 * reintentos. No necesita Ollama ni internet.
 */
@ExtendWith(OutputCaptureExtension.class)
class OllamaModeloConversacionalTest {

    private static final String RESPUESTA_OK = """
            {"model":"gemma4:cloud","created_at":"2026-11-05T10:00:00Z",\
            "message":{"role":"assistant","content":"%s","thinking":"%s"},"done":true,\
            "total_duration":1000000,"prompt_eval_count":42,"eval_count":7}""";

    private HttpServer servidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String cuerpoDeRespuesta = RESPUESTA_OK.formatted("Hola, soy Alma.", "pienso algo");
    private volatile long demoraMs = 0;
    private ExecutorService hilosDelServidor;

    @BeforeEach
    void iniciarServidor() throws IOException {
        hilosDelServidor = Executors.newCachedThreadPool();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(hilosDelServidor);
        servidor.createContext("/api/chat", intercambio -> {
            pedidos.incrementAndGet();
            cuerpos.add(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = cuerpoDeRespuesta.getBytes(StandardCharsets.UTF_8);
            try {
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(estado, bytes.length);
                intercambio.getResponseBody().write(bytes);
            } catch (IOException e) {
                // El cliente ya cortó la conexión por tiempo: es lo que se prueba.
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
        hilosDelServidor.shutdownNow();
    }

    private OllamaModeloConversacional modelo(Duration timeout, Duration esperaTurno) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        OllamaChatModel chatModel = ConfiguracionIa.crearChatModel(ConfiguracionIa.crearOllamaApi(url, timeout),
                "gemma4:cloud");
        return new OllamaModeloConversacional(chatModel, new LimitadorDePedidos(esperaTurno), "gemma4:cloud", 0.6,
                400);
    }

    /** Chat con su tope y extracción con otro: dos clientes contra el mismo servidor falso. */
    private OllamaModeloConversacional modeloConDosClientes(Duration timeoutChat, Duration timeoutExtraccion) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        OllamaChatModel chat = ConfiguracionIa.crearChatModel(ConfiguracionIa.crearOllamaApi(url, timeoutChat),
                "gemma4:cloud");
        OllamaChatModel extraccion = ConfiguracionIa
                .crearChatModel(ConfiguracionIa.crearOllamaApi(url, timeoutExtraccion), "gemma4:cloud");
        return new OllamaModeloConversacional(chat, extraccion, new LimitadorDePedidos(Duration.ofSeconds(2)),
                "gemma4:cloud", 0.6, 400);
    }

    private OllamaModeloConversacional modelo() {
        return modelo(Duration.ofSeconds(5), Duration.ofSeconds(2));
    }

    private static final List<TurnoConversacion> TURNOS = List.of(
            new TurnoConversacion(TurnoConversacion.Rol.ALMA, "¿Cómo viene tu día?"),
            new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Bien, gracias"));

    @Test
    void mandaElPedidoConRazonamientoDesactivadoYLosParametros() {
        String respuesta = modelo().responder("INSTRUCCIONES", TURNOS);

        assertEquals("Hola, soy Alma.", respuesta);
        assertEquals(1, pedidos.get());
        String cuerpo = cuerpos.get(0);
        assertTrue(cuerpo.contains("\"think\":false"), cuerpo);
        assertTrue(cuerpo.contains("\"temperature\":0.6"), cuerpo);
        assertTrue(cuerpo.contains("\"num_predict\":400"), cuerpo);
        assertTrue(cuerpo.contains("\"model\":\"gemma4:cloud\""), cuerpo);
        assertTrue(cuerpo.contains("\"stream\":false"), cuerpo);
        // Sistema, turno de Alma como "assistant" y el mensaje de la persona al final.
        int sistema = cuerpo.indexOf("\"role\":\"system\"");
        int alma = cuerpo.indexOf("\"role\":\"assistant\"");
        int persona = cuerpo.indexOf("\"role\":\"user\"");
        assertTrue(sistema >= 0 && sistema < alma && alma < persona, cuerpo);
    }

    @Test
    void ignoraElCampoDeRazonamientoSeparado() {
        assertEquals("Hola, soy Alma.", modelo().responder("I", TURNOS));
    }

    @Test
    void quitaUnBloqueDeRazonamientoQueVengaEnElTexto() {
        cuerpoDeRespuesta = RESPUESTA_OK.formatted("<think>razono</think>  Qué lindo.  ", "");
        assertEquals("Qué lindo.", modelo().responder("I", TURNOS));
    }

    @Test
    void unaRespuestaVaciaEsUnaFalla() {
        cuerpoDeRespuesta = RESPUESTA_OK.formatted("   ", "");
        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));
        assertEquals(1, pedidos.get());
    }

    @Test
    void unError500EsUnaFallaYSeHaceUnSoloPedidoSinReintentos() {
        estado = 500;
        cuerpoDeRespuesta = "{\"error\":\"fallo interno\"}";
        OllamaModeloConversacional modelo = modelo();

        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));

        assertEquals(1, pedidos.get(), "No debe reintentar");
    }

    @Test
    void unError402SinCreditosEsUnaFallaSinReintentos() {
        estado = 402;
        cuerpoDeRespuesta = "{\"error\":\"payment required\"}";

        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));

        assertEquals(1, pedidos.get());
    }

    @Test
    void sinServidorEsUnaFalla() {
        servidor.stop(0);
        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));
    }

    @Test
    void sePasaDeTiempoSeCortaYNoSeReintenta() {
        demoraMs = 3000;
        OllamaModeloConversacional modelo = modelo(Duration.ofMillis(700), Duration.ofSeconds(2));

        long inicio = System.nanoTime();
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));
        long duracionMs = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(duracionMs < 2500, "Debió cortar cerca de 700 ms y tardó " + duracionMs);
        assertEquals(1, pedidos.get(), "No debe reintentar tras el corte");
    }

    @Test
    void elTurnoSeLiberaDespuesDeUnaFalla() {
        OllamaModeloConversacional modelo = modelo();
        estado = 500;
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));

        estado = 200;
        assertEquals("Hola, soy Alma.", modelo.responder("I", TURNOS));
    }

    @Test
    void unPedidoALaVezElSegundoEsperaYSiNoConsigueTurnoFalla() throws Exception {
        demoraMs = 1200;
        OllamaModeloConversacional modelo = modelo(Duration.ofSeconds(5), Duration.ofMillis(300));
        ExecutorService hilos = Executors.newSingleThreadExecutor();
        try {
            Future<String> primero = hilos.submit(() -> modelo.responder("I", TURNOS));
            while (pedidos.get() == 0) {
                Thread.sleep(20);
            }

            assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));
            assertEquals("Hola, soy Alma.", primero.get(5, TimeUnit.SECONDS));
            // El primero ya terminó: el turno está libre otra vez.
            assertEquals("Hola, soy Alma.", modelo.responder("I", TURNOS));
            assertEquals(2, pedidos.get(), "El pedido sin turno nunca llegó al servidor");
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void elLogRegistraDuracionYTokensPeroNoElTexto(CapturedOutput salida) {
        modelo().responder("INSTRUCCIONES_SECRETAS", List.of(
                new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "MENSAJE_SECRETO")));

        assertTrue(salida.getAll().contains("tokens de entrada: 42"), salida.getAll());
        assertTrue(salida.getAll().contains("de salida: 7"), salida.getAll());
        assertFalse(salida.getAll().contains("MENSAJE_SECRETO"));
        assertFalse(salida.getAll().contains("INSTRUCCIONES_SECRETAS"));
        assertFalse(salida.getAll().contains("Hola, soy Alma."));
    }

    @Test
    void unaFallaNoEscribeElTextoEnElLog(CapturedOutput salida) {
        estado = 500;
        cuerpoDeRespuesta = "{\"error\":\"eco MENSAJE_SECRETO\"}";

        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("INSTRUCCIONES_SECRETAS",
                List.of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "MENSAJE_SECRETO"))));

        assertFalse(salida.getAll().contains("MENSAJE_SECRETO"), salida.getAll());
        assertFalse(salida.getAll().contains("INSTRUCCIONES_SECRETAS"), salida.getAll());
    }

    @Test
    void conOpcionesElPedidoLlevaLosParametrosDeEsaLlamadaYElChatConservaLosSuyos() {
        OllamaModeloConversacional modelo = modelo();

        String respuesta = modelo.responder("I", TURNOS, new ModeloConversacional.OpcionesDeLlamada(0.1, 600));
        String delChat = modelo.responder("I", TURNOS);

        assertEquals("Hola, soy Alma.", respuesta);
        assertEquals("Hola, soy Alma.", delChat);
        String extraccion = cuerpos.get(0);
        assertTrue(extraccion.contains("\"temperature\":0.1"), extraccion);
        assertTrue(extraccion.contains("\"num_predict\":600"), extraccion);
        assertTrue(extraccion.contains("\"think\":false"), extraccion);
        String chat = cuerpos.get(1);
        assertTrue(chat.contains("\"temperature\":0.6"), chat);
        assertTrue(chat.contains("\"num_predict\":400"), chat);
    }

    @Test
    void conOpcionesUsaElClienteDeExtraccionConSuPropioTope() {
        demoraMs = 1500;
        // El chat aguanta 5 s; la extracción corta a los 500 ms.
        OllamaModeloConversacional modelo = modeloConDosClientes(Duration.ofSeconds(5), Duration.ofMillis(500));

        assertThrows(ModeloNoDisponibleException.class,
                () -> modelo.responder("I", TURNOS, new ModeloConversacional.OpcionesDeLlamada(0.1, 600)));
        assertEquals("Hola, soy Alma.", modelo.responder("I", TURNOS), "El chat conserva su tope");
        assertEquals(2, pedidos.get(), "Sin reintentos");
    }

    @Test
    void elTopeDelChatNoCortaLaExtraccionMasLarga() {
        demoraMs = 1000;
        OllamaModeloConversacional modelo = modeloConDosClientes(Duration.ofMillis(400), Duration.ofSeconds(5));

        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));
        assertEquals("Hola, soy Alma.",
                modelo.responder("I", TURNOS, new ModeloConversacional.OpcionesDeLlamada(0.1, 600)));
    }

    @Test
    void conOpcionesElLogNoEscribeElTexto(CapturedOutput salida) {
        estado = 500;
        cuerpoDeRespuesta = "{\"error\":\"eco MENSAJE_SECRETO\"}";

        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("INSTRUCCIONES_SECRETAS",
                List.of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "MENSAJE_SECRETO")),
                new ModeloConversacional.OpcionesDeLlamada(0.1, 600)));

        assertFalse(salida.getAll().contains("MENSAJE_SECRETO"), salida.getAll());
        assertFalse(salida.getAll().contains("INSTRUCCIONES_SECRETAS"), salida.getAll());
    }

    @Test
    void sinSerSobrescritoElMetodoConOpcionesDelegaEnElDeSiempre() {
        List<String> vistos = new java.util.ArrayList<>();
        ModeloConversacional minimo = (instrucciones, turnos) -> {
            vistos.add(instrucciones);
            return "ok";
        };

        assertEquals("ok", minimo.responder("X", TURNOS, new ModeloConversacional.OpcionesDeLlamada(0.1, 600)));
        assertEquals(List.of("X"), vistos);
    }
}
