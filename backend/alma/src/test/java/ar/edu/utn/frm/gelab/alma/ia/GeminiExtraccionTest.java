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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.google.genai.Client;
import com.sun.net.httpserver.HttpServer;

import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional.OpcionesDeLlamada;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * La extracción de recuerdos con Gemini (contrato del grafo, 5.4, y contrato de IA) contra un servidor falso:
 * parámetros propios (temperatura, tope de tokens y salida JSON), tope de 25 s por llamada, turno en segundo
 * plano detrás del chat, rechazo de {@code responseMimeType} y logs sin textos de la persona.
 */
@ExtendWith(OutputCaptureExtension.class)
class GeminiExtraccionTest {

    private static final String CLAVE = "clave-de-prueba-no-real";
    private static final String MODELO = "gemini-de-prueba";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final OpcionesDeLlamada OPCIONES = new OpcionesDeLlamada(0.1, 600);
    private static final List<TurnoConversacion> TURNOS = List
            .of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Pregunta: ¿Y tu casa?\nRespuesta: Era grande."));

    private static final String RESPUESTA = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"%s"}]},"finishReason":"STOP","index":0}],\
            "usageMetadata":{"promptTokenCount":42,"candidatesTokenCount":7,"thoughtsTokenCount":%d,"totalTokenCount":49},\
            "modelVersion":"gemini-de-prueba"}""";
    private static final String JSON_DEL_RECUERDO = "{\\\"titulo\\\":\\\"La casa\\\",\\\"descripcion\\\":\\\"Era grande.\\\"}";

    private HttpServer servidor;
    private ExecutorService hilosDelServidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private volatile long demoraMs = 0;
    private volatile boolean rechazarTipoJson = false;
    /** Si es mayor que 0, el servidor responde con ese código HTTP de error (por ejemplo, 503). */
    private volatile int codigoDeError = 0;
    private volatile int tokensDeRazonamiento = 0;

    @BeforeEach
    void iniciarServidor() throws IOException {
        hilosDelServidor = Executors.newCachedThreadPool();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(hilosDelServidor);
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            cuerpos.add(cuerpo);
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int estado = 200;
            String respuesta = RESPUESTA.formatted(JSON_DEL_RECUERDO, tokensDeRazonamiento);
            if (codigoDeError > 0) {
                estado = codigoDeError;
                respuesta = "{\"error\":{\"code\":" + codigoDeError + ",\"message\":\"error de prueba\","
                        + "\"status\":\"UNAVAILABLE\"}}";
            } else if (rechazarTipoJson && cuerpo.contains("responseMimeType")) {
                estado = 400;
                respuesta = "{\"error\":{\"code\":400,\"message\":\"response_mime_type is not supported by this model\","
                        + "\"status\":\"INVALID_ARGUMENT\"}}";
            }
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
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

    private GoogleGenAiChatModel chatModel(Duration timeout) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        Client cliente = ConfiguracionIa.crearClienteGemini(CLAVE, url, timeout);
        return ConfiguracionIa.crearChatModelGemini(cliente, MODELO);
    }

    private GeminiModeloConversacional modelo(Duration timeoutChat, Duration timeoutExtraccion,
            Duration esperaTurno) {
        return new GeminiModeloConversacional(chatModel(timeoutChat), chatModel(timeoutExtraccion),
                new LimitadorDePedidos(esperaTurno), MODELO, 0.6, 400, 1500);
    }

    private GeminiModeloConversacional modelo() {
        return modelo(Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofSeconds(2));
    }

    @Test
    void elPedidoDeExtraccionLlevaSusParametrosYPideJson() {
        String salida = modelo().responder("PROMPT-EXTRACTOR", TURNOS, OPCIONES);

        assertTrue(salida.contains("\"titulo\""), salida);
        assertEquals(1, pedidos.get());
        JsonNode configuracion = JSON.readTree(cuerpos.get(0)).get("generationConfig");
        assertEquals(0.1, configuracion.get("temperature").asDouble(), 0.001);
        assertEquals("application/json", configuracion.get("responseMimeType").asString());
        // Sin razonamiento configurado: el tope sube solo para Gemini (piso de 1500) por si esos tokens cuentan.
        assertEquals(1500, configuracion.get("maxOutputTokens").asInt());
        assertFalse(cuerpos.get(0).contains("thinkingConfig"), cuerpos.get(0));
    }

    @Test
    void elPisoDelTopeNoBajaUnTopeMasAltoDeLasOpciones() {
        modelo().responder("P", TURNOS, new OpcionesDeLlamada(0.1, 3000));

        assertEquals(3000, JSON.readTree(cuerpos.get(0)).get("generationConfig").get("maxOutputTokens").asInt());
    }

    @Test
    void elChatSigueConSusParametrosSinPedirJson() {
        modelo().responder("INSTRUCCIONES", TURNOS);

        JsonNode configuracion = JSON.readTree(cuerpos.get(0)).get("generationConfig");
        assertEquals(0.6, configuracion.get("temperature").asDouble(), 0.001);
        assertEquals(400, configuracion.get("maxOutputTokens").asInt());
        assertFalse(cuerpos.get(0).contains("responseMimeType"), cuerpos.get(0));
    }

    // ---------- Errores transitorios (contrato del grafo, 5.4, paso 6) ----------

    private ModeloNoDisponibleException falla(int codigoHttp) {
        codigoDeError = codigoHttp;
        return assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("P", TURNOS, OPCIONES));
    }

    @Test
    void unaRespuestaConCodigoTransitorioDelProveedorSeMarcaComoTransitoria() {
        for (int codigo : new int[] { 429, 500, 502, 503, 504 }) {
            assertTrue(falla(codigo).esTransitorio(), "El " + codigo + " es transitorio");
        }
    }

    @Test
    void unaRespuestaConCodigoNoTransitorioNoSeMarcaComoTransitoria() {
        for (int codigo : new int[] { 400, 401, 402, 403, 404 }) {
            assertFalse(falla(codigo).esTransitorio(), "El " + codigo + " no es transitorio");
        }
    }

    @Test
    void elMensajeDelErrorTransitorioNoCitaLaRespuestaDelProveedor() {
        ModeloNoDisponibleException error = falla(503);

        assertFalse(error.getMessage().contains("error de prueba"), error.getMessage());
    }

    @Test
    void unCortePorTiempoNoEsTransitorio() {
        demoraMs = 3000;
        GeminiModeloConversacional modelo = modelo(Duration.ofSeconds(5), Duration.ofMillis(500), Duration.ofSeconds(2));

        ModeloNoDisponibleException error = assertThrows(ModeloNoDisponibleException.class,
                () -> modelo.responder("P", TURNOS, OPCIONES));

        assertFalse(error.esTransitorio());
    }

    @Test
    void laClaveFaltanteNoEsTransitoria() {
        GeminiModeloConversacional sinClave = new GeminiModeloConversacional(null, null, new LimitadorDePedidos(Duration.ofSeconds(1)),
                MODELO, 0.6, 400, 1500);

        ModeloNoDisponibleException error = assertThrows(ModeloNoDisponibleException.class,
                () -> sinClave.responder("P", TURNOS, OPCIONES));

        assertFalse(error.esTransitorio());
    }

    @Test
    void laExtraccionUsaSuPropioTopeDeTiempoYNoReintenta() throws Exception {
        demoraMs = 4000;
        // El chat tiene tiempo de sobra; la extracción, 1 s.
        GeminiModeloConversacional modelo = modelo(Duration.ofSeconds(10), Duration.ofSeconds(1),
                Duration.ofSeconds(2));

        long inicio = System.nanoTime();
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("P", TURNOS, OPCIONES));
        long milis = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(milis >= 900 && milis < 3000, "Tardó " + milis + " ms: tenía que cortarse al segundo");
        Thread.sleep(1500);
        assertEquals(1, pedidos.get(), "Un corte por tiempo no se reintenta");
    }

    @Test
    void laExtraccionEsperaSuTurnoDetrasDelChatSinElTopeDeEsperaDelChat() throws Exception {
        demoraMs = 900;
        // Un chat espera como máximo 300 ms; la extracción no tiene tope de espera.
        GeminiModeloConversacional modelo = modelo(Duration.ofSeconds(5), Duration.ofSeconds(5),
                Duration.ofMillis(300));
        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            var chat = hilo.submit(() -> modelo.responder("I", TURNOS));
            Thread.sleep(200); // el chat ya tiene el turno
            String salida = modelo.responder("P", TURNOS, OPCIONES);
            assertTrue(salida.contains("\"titulo\""), salida);
            chat.get();
        } finally {
            hilo.shutdownNow();
        }
        assertEquals(2, pedidos.get());
    }

    @Test
    void siLaApiRechazaElTipoJsonSeRepiteSinEseCampoYSeRecuerda(CapturedOutput salida) {
        rechazarTipoJson = true;
        GeminiModeloConversacional modelo = modelo();

        String primera = modelo.responder("P", TURNOS, OPCIONES);

        assertTrue(primera.contains("\"titulo\""), primera);
        assertEquals(2, pedidos.get());
        assertTrue(cuerpos.get(0).contains("responseMimeType"));
        assertFalse(cuerpos.get(1).contains("responseMimeType"), "El reintento va sin el campo");
        // Se recuerda: el pedido siguiente ya sale sin el campo y es uno solo.
        modelo.responder("P", TURNOS, OPCIONES);
        assertEquals(3, pedidos.get());
        assertFalse(cuerpos.get(2).contains("responseMimeType"));

        String log = salida.getOut() + salida.getErr();
        assertTrue(log.contains("rechazó responseMimeType"), log);
        assertFalse(log.contains("is not supported by this model"), "El mensaje del error no se registra");
        assertFalse(log.contains("Era grande"), log);
    }

    @Test
    void unRechazoQueNoEsDelTipoJsonNoSeReintenta() {
        rechazarTipoJson = false;
        servidor.removeContext("/");
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            intercambio.getRequestBody().readAllBytes();
            byte[] bytes = "{\"error\":{\"code\":400,\"message\":\"otro problema\",\"status\":\"INVALID_ARGUMENT\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(400, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });

        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("P", TURNOS, OPCIONES));

        assertEquals(1, pedidos.get());
    }

    @Test
    void sinClaveLaExtraccionFallaSinLlamarALaRed() {
        GeminiModeloConversacional sinClave = new GeminiModeloConversacional(null, null,
                new LimitadorDePedidos(Duration.ofSeconds(1)), MODELO, 0.6, 400, 1500);

        assertThrows(ModeloNoDisponibleException.class, () -> sinClave.responder("P", TURNOS, OPCIONES));

        assertEquals(0, pedidos.get());
    }

    @Test
    void elLogDistingueLaExtraccionInformaLosTokensDeRazonamientoYNoTieneTextos(CapturedOutput salida) {
        tokensDeRazonamiento = 120;

        modelo().responder("PROMPT-SECRETO", List.of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA,
                "TEXTO-DE-LA-PERSONA-SECRETO")), OPCIONES);

        String log = salida.getOut() + salida.getErr();
        assertTrue(log.contains("Gemini (extracción) respondió en"), log);
        assertTrue(log.contains("de razonamiento: 120"), log);
        for (String secreto : List.of("PROMPT-SECRETO", "TEXTO-DE-LA-PERSONA-SECRETO", "La casa", CLAVE)) {
            assertFalse(log.contains(secreto), "El log no debe contener " + secreto);
        }
    }
}
