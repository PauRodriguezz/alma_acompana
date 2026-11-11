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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.google.genai.Client;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link GeminiModeloConversacional} contra un servidor falso que imita a Gemini (contrato de IA, sección 6):
 * el pedido que sale, la limpieza, el razonamiento descartado, los errores sin reintentos, el corte por
 * tiempo, la ausencia de clave, la conversación que empieza con turnos de Alma y que los logs no tengan
 * textos ni la clave. No necesita internet ni una clave real. Las respuestas del servidor falso tienen que
 * incluir {@code modelVersion}: Spring AI lo lee sin comprobar que exista.
 */
@ExtendWith(OutputCaptureExtension.class)
class GeminiModeloConversacionalTest {

    private static final String CLAVE = "clave-de-prueba-no-real";
    private static final String MODELO = "gemini-de-prueba";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String RESPUESTA = """
            {"candidates":[{"content":{"role":"model","parts":[%s]},"finishReason":"%s","index":0}],\
            "usageMetadata":{"promptTokenCount":42,"candidatesTokenCount":7,"totalTokenCount":49},\
            "modelVersion":"gemini-de-prueba"}""";
    private static final String PARTE_RAZONAMIENTO = "{\"text\":\"pienso algo\",\"thought\":true}";

    private HttpServer servidor;
    private ExecutorService hilosDelServidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private final List<String> clavesEnCabecera = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile long demoraMs = 0;
    private volatile String cuerpoDeRespuesta = respuesta(PARTE_RAZONAMIENTO + ",{\"text\":\"  Hola, soy Alma.  \"}",
            "STOP");

    private static String respuesta(String partes, String motivoDeFin) {
        return RESPUESTA.formatted(partes, motivoDeFin);
    }

    @BeforeEach
    void iniciarServidor() throws IOException {
        hilosDelServidor = Executors.newCachedThreadPool();
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(hilosDelServidor);
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            rutas.add(intercambio.getRequestURI().toString());
            String clave = intercambio.getRequestHeaders().getFirst("x-goog-api-key");
            clavesEnCabecera.add(clave == null ? "" : clave);
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

    private GeminiModeloConversacional modelo(Duration timeout, Duration esperaTurno) {
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        Client cliente = ConfiguracionIa.crearClienteGemini(CLAVE, url, timeout);
        GoogleGenAiChatModel chatModel = ConfiguracionIa.crearChatModelGemini(cliente, MODELO);
        return new GeminiModeloConversacional(chatModel, new LimitadorDePedidos(esperaTurno), MODELO, 0.6, 400);
    }

    private GeminiModeloConversacional modelo() {
        return modelo(Duration.ofSeconds(5), Duration.ofSeconds(2));
    }

    private static TurnoConversacion persona(String texto) {
        return new TurnoConversacion(TurnoConversacion.Rol.PERSONA, texto);
    }

    private static TurnoConversacion alma(String texto) {
        return new TurnoConversacion(TurnoConversacion.Rol.ALMA, texto);
    }

    /** Un caso normal: el primer turno es de la persona. */
    private static final List<TurnoConversacion> TURNOS = List.of(persona("Hola"), alma("Hola, soy Alma."),
            persona("Bien, gracias"));

    private JsonNode cuerpoEnviado(int numero) {
        return JSON.readTree(cuerpos.get(numero));
    }

    private static String textoDeInstrucciones(JsonNode cuerpo) {
        return cuerpo.get("systemInstruction").get("parts").get(0).get("text").asString();
    }

    // --- El pedido que sale ---

    @Test
    void mandaElPedidoConModeloSistemaTurnosEnOrdenParametrosYClaveEnLaCabecera() {
        String respuesta = modelo().responder("INSTRUCCIONES", TURNOS);

        assertEquals("Hola, soy Alma.", respuesta);
        assertEquals(1, pedidos.get());
        String ruta = rutas.get(0);
        assertTrue(ruta.contains("/models/" + MODELO + ":generateContent"), ruta);
        // La clave va en la cabecera, no en la URL.
        assertEquals(CLAVE, clavesEnCabecera.get(0));
        assertFalse(ruta.contains(CLAVE), ruta);

        JsonNode cuerpo = cuerpoEnviado(0);
        assertEquals("INSTRUCCIONES", textoDeInstrucciones(cuerpo));
        JsonNode contenidos = cuerpo.get("contents");
        assertEquals(3, contenidos.size());
        String[] roles = { "user", "model", "user" };
        String[] textos = { "Hola", "Hola, soy Alma.", "Bien, gracias" };
        for (int i = 0; i < 3; i++) {
            assertEquals(roles[i], contenidos.get(i).get("role").asString(), cuerpo.toString());
            assertEquals(textos[i], contenidos.get(i).get("parts").get(0).get("text").asString());
        }
        JsonNode configuracion = cuerpo.get("generationConfig");
        assertEquals(0.6, configuracion.get("temperature").asDouble(), 0.001);
        assertEquals(400, configuracion.get("maxOutputTokens").asInt());
        // No se pide razonamiento ni se configura su nivel.
        assertFalse(cuerpo.toString().contains("thinkingConfig"), cuerpo.toString());
    }

    // --- La respuesta ---

    @Test
    void devuelveElTextoSinEspaciosEnLosExtremos() {
        cuerpoDeRespuesta = respuesta("{\"text\":\"  \\n Hola, soy Alma. \\n \"}", "STOP");
        assertEquals("Hola, soy Alma.", modelo().responder("I", TURNOS));
    }

    @Test
    void descartaLasPartesDeRazonamientoAunqueVenganPrimeras() {
        // Es el caso por defecto del servidor falso: razonamiento primero y texto final después.
        assertEquals("Hola, soy Alma.", modelo().responder("I", TURNOS));
    }

    @Test
    void descartaLasPartesDeRazonamientoAunqueVenganDespues() {
        cuerpoDeRespuesta = respuesta("{\"text\":\"Qué lindo.\"}," + PARTE_RAZONAMIENTO, "STOP");
        assertEquals("Qué lindo.", modelo().responder("I", TURNOS));
    }

    @Test
    void unaRespuestaConSoloRazonamientoEsUnaFalla() {
        cuerpoDeRespuesta = respuesta(PARTE_RAZONAMIENTO, "STOP");
        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));
        assertEquals(1, pedidos.get());
    }

    @Test
    void unTextoFinalVacioEsUnaFalla() {
        cuerpoDeRespuesta = respuesta("{\"text\":\"   \"}", "STOP");
        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));
        assertEquals(1, pedidos.get());
    }

    @Test
    void unaRespuestaSinCandidatosEsUnaFalla() {
        cuerpoDeRespuesta = "{\"candidates\":[],\"modelVersion\":\"gemini-de-prueba\"}";
        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));
        assertEquals(1, pedidos.get());
    }

    @Test
    void siTerminaPorMaxTokensSeUsaElTextoYSeAvisaEnElLogSinElTexto(CapturedOutput salida) {
        cuerpoDeRespuesta = respuesta("{\"text\":\"TEXTO-CORTADO-POR-EL-TOPE\"}", "MAX_TOKENS");

        assertEquals("TEXTO-CORTADO-POR-EL-TOPE", modelo().responder("I", TURNOS));

        assertTrue(salida.getOut().contains("MAX_TOKENS"), salida.getOut());
        assertFalse(salida.getOut().contains("TEXTO-CORTADO-POR-EL-TOPE"), salida.getOut());
    }

    // --- Errores ---

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 429, 500 })
    void cadaErrorDelServicioEsUnaFallaYSeHaceUnSoloPedidoSinReintentos(int codigo) {
        estado = codigo;
        cuerpoDeRespuesta = "{\"error\":{\"code\":" + codigo + ",\"message\":\"fallo\",\"status\":\"ERROR\"}}";

        assertThrows(ModeloNoDisponibleException.class, () -> modelo().responder("I", TURNOS));

        assertEquals(1, pedidos.get(), "No debe reintentar ante un " + codigo);
    }

    @Test
    void unaDemoraMayorAlTopeSeCortaATiempoSinReintentarYLiberaElTurno() throws Exception {
        demoraMs = 4000;
        GeminiModeloConversacional modelo = modelo(Duration.ofSeconds(1), Duration.ofMillis(500));

        long inicio = System.nanoTime();
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", TURNOS));
        long milis = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(milis >= 900 && milis < 3000, "Tardó " + milis + " ms: tenía que cortarse al segundo");
        // Un reintento del SDK esperaría al menos ~1 s más: si no hubo, sigue siendo un solo pedido.
        Thread.sleep(1500);
        assertEquals(1, pedidos.get(), "Un corte por tiempo no se reintenta");

        // El turno se liberó: otro pedido, con el mismo limitador, se atiende en vez de fallar por espera.
        demoraMs = 0;
        assertEquals("Hola, soy Alma.", modelo.responder("I", TURNOS));
        assertEquals(2, pedidos.get());
    }

    // --- Sin clave ---

    @Test
    void sinClaveFallaSinHacerNingunaLlamadaDeRed() {
        GeminiModeloConversacional sinClave = new GeminiModeloConversacional(null,
                new LimitadorDePedidos(Duration.ofSeconds(2)), MODELO, 0.6, 400);

        assertThrows(ModeloNoDisponibleException.class, () -> sinClave.responder("I", TURNOS));

        assertEquals(0, pedidos.get());
    }

    // --- Conversación que empieza con turnos de Alma ---

    @Test
    void losTurnosDeAlmaDelPrincipioPasanALasInstruccionesYNoSeMandanComoMensajes() {
        List<TurnoConversacion> turnos = List.of(alma("¿Cómo te sentís hoy?"), persona("Bien, gracias"));

        modelo().responder("INSTRUCCIONES", turnos);

        JsonNode cuerpo = cuerpoEnviado(0);
        assertEquals("INSTRUCCIONES\nLo último que le dijiste a la persona antes de este mensaje: ¿Cómo te sentís hoy?",
                textoDeInstrucciones(cuerpo));
        JsonNode contenidos = cuerpo.get("contents");
        assertEquals(1, contenidos.size(), cuerpo.toString());
        assertEquals("user", contenidos.get(0).get("role").asString());
        assertEquals("Bien, gracias", contenidos.get(0).get("parts").get(0).get("text").asString());
    }

    @Test
    void variosTurnosDeAlmaDelPrincipioVanUnoPorLineaYEnOrden() {
        List<TurnoConversacion> turnos = List.of(alma("Primero"), alma("Segundo"), persona("Hola"),
                alma("Respuesta"), persona("Gracias"));

        modelo().responder("INSTRUCCIONES\n", turnos);

        JsonNode cuerpo = cuerpoEnviado(0);
        assertEquals("INSTRUCCIONES\n"
                + "Lo último que le dijiste a la persona antes de este mensaje: Primero\n"
                + "Lo último que le dijiste a la persona antes de este mensaje: Segundo",
                textoDeInstrucciones(cuerpo));
        // Solo se mueven los del principio: de ahí en adelante los turnos salen como siempre.
        JsonNode contenidos = cuerpo.get("contents");
        assertEquals(3, contenidos.size(), cuerpo.toString());
        assertEquals("user", contenidos.get(0).get("role").asString());
        assertEquals("model", contenidos.get(1).get("role").asString());
        assertEquals("Respuesta", contenidos.get(1).get("parts").get(0).get("text").asString());
        assertEquals("user", contenidos.get(2).get("role").asString());
    }

    @Test
    void enElCasoNormalLasInstruccionesYLosTurnosSalenSinCambios() {
        modelo().responder("INSTRUCCIONES\ncon dos líneas\n", TURNOS);

        JsonNode cuerpo = cuerpoEnviado(0);
        assertEquals("INSTRUCCIONES\ncon dos líneas\n", textoDeInstrucciones(cuerpo));
        assertFalse(textoDeInstrucciones(cuerpo).contains("Lo último que le dijiste"));
        assertEquals(3, cuerpo.get("contents").size());
    }

    @Test
    void sinNingunTurnoDeLaPersonaFallaSinLlamarALaRed() {
        GeminiModeloConversacional modelo = modelo();

        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", List.of(alma("Hola"))));
        assertThrows(ModeloNoDisponibleException.class,
                () -> modelo.responder("I", List.of(alma("Uno"), alma("Dos"))));
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I", List.of()));

        assertEquals(0, pedidos.get());
    }

    // --- Logs ---

    @Test
    void losLogsNoTienenTextosNiLaClaveNiElMensajeDeLosErrores(CapturedOutput salida) {
        cuerpoDeRespuesta = respuesta("{\"text\":\"RESPUESTA-SECRETA\"}", "STOP");
        List<TurnoConversacion> turnos = List.of(alma("TURNO-DE-ALMA-SECRETO"), persona("TEXTO-DE-LA-PERSONA-SECRETO"));
        GeminiModeloConversacional modelo = modelo();

        modelo.responder("INSTRUCCIONES-SECRETAS", turnos);
        estado = 500;
        cuerpoDeRespuesta = "{\"error\":{\"code\":500,\"message\":\"MENSAJE-DE-ERROR-SECRETO\",\"status\":\"INTERNAL\"}}";
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("INSTRUCCIONES-SECRETAS", turnos));
        assertThrows(ModeloNoDisponibleException.class,
                () -> modelo.responder("INSTRUCCIONES-SECRETAS", List.of(alma("TURNO-DE-ALMA-SECRETO"))));
        new GeminiModeloConversacional(null, new LimitadorDePedidos(Duration.ofSeconds(1)), MODELO, 0.6, 400);

        String log = salida.getOut() + salida.getErr();
        // Se comprueba que el log existe, para que la prueba no sea vacía.
        assertTrue(log.contains("Gemini respondió en"), log);
        assertTrue(log.contains("código HTTP: 500"), log);
        assertTrue(log.contains("Falta GEMINI_API_KEY"), log);
        for (String secreto : List.of("RESPUESTA-SECRETA", "TURNO-DE-ALMA-SECRETO", "TEXTO-DE-LA-PERSONA-SECRETO",
                "INSTRUCCIONES-SECRETAS", "MENSAJE-DE-ERROR-SECRETO", CLAVE)) {
            assertFalse(log.contains(secreto), "El log no debe contener " + secreto);
        }
    }
}
