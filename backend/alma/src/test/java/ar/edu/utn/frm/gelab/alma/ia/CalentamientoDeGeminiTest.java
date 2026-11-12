package ar.edu.utn.frm.gelab.alma.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.google.genai.Client;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * El calentamiento de Gemini contra un servidor falso: un {@code generateContent} de 1 token por modelo con un texto
 * fijo, ningún otro tipo de llamada, la clave en la cabecera, turno cedido al chat, sin romper nada si falla y sin
 * registrar mensajes de error ni la clave. Sin internet ni clave real.
 */
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class CalentamientoDeGeminiTest {

    private static final String CLAVE = "clave-de-prueba-no-real";
    private static final String MODELO = "gemini-de-prueba";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String RESPUESTA = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"H"}]},"finishReason":"MAX_TOKENS","index":0}],\
            "usageMetadata":{"promptTokenCount":3,"candidatesTokenCount":1,"totalTokenCount":4},\
            "modelVersion":"gemini-de-prueba"}""";

    private HttpServer servidor;
    private final AtomicInteger pedidos = new AtomicInteger();
    private final List<String> metodosYRutas = new CopyOnWriteArrayList<>();
    private final List<String> cuerpos = new CopyOnWriteArrayList<>();
    private final List<String> claves = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.createContext("/", intercambio -> {
            pedidos.incrementAndGet();
            metodosYRutas.add(intercambio.getRequestMethod() + " " + intercambio.getRequestURI());
            String clave = intercambio.getRequestHeaders().getFirst("x-goog-api-key");
            claves.add(clave == null ? "" : clave);
            cuerpos.add(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String cuerpo = estado == 200 ? RESPUESTA
                    : "{\"error\":{\"code\":" + estado + ",\"message\":\"MENSAJE-SECRETO-DEL-SERVICIO\","
                            + "\"status\":\"ERROR\"}}";
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private GoogleGenAiChatModel modelo() {
        Client cliente = ConfiguracionIa.crearClienteGemini(CLAVE,
                "http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofSeconds(5));
        return ConfiguracionIa.crearChatModelGemini(cliente, MODELO);
    }

    private CalentamientoDeGemini calentamiento(int cantidad, LimitadorDePedidos limitador) {
        List<GoogleGenAiChatModel> modelos = new java.util.ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            modelos.add(modelo());
        }
        return new CalentamientoDeGemini(modelos, limitador, MODELO);
    }

    private CalentamientoDeGemini calentamiento(int cantidad) {
        return calentamiento(cantidad, new LimitadorDePedidos(Duration.ofSeconds(2)));
    }

    // --- Qué manda ---

    @Test
    void haceUnaGeneracionDeUnTokenPorModeloConUnTextoFijoYNadaMas(CapturedOutput salida) {
        calentamiento(2).calentar();

        assertEquals(2, pedidos.get());
        for (int i = 0; i < 2; i++) {
            String pedido = metodosYRutas.get(i);
            assertTrue(pedido.startsWith("POST "), pedido);
            assertTrue(pedido.contains("/models/" + MODELO + ":generateContent"), pedido);
            assertFalse(pedido.contains(CLAVE), "La clave no va en la URL: " + pedido);
            assertEquals(CLAVE, claves.get(i));

            JsonNode cuerpo = JSON.readTree(cuerpos.get(i));
            assertEquals(1, cuerpo.get("contents").size(), cuerpo.toString());
            assertEquals("user", cuerpo.get("contents").get(0).get("role").asString());
            assertEquals(CalentamientoDeGemini.TEXTO_DE_CALENTAMIENTO,
                    cuerpo.get("contents").get(0).get("parts").get(0).get("text").asString());
            assertEquals(1, cuerpo.get("generationConfig").get("maxOutputTokens").asInt());
            // Nada de instrucciones de sistema, razonamiento ni salida JSON.
            assertFalse(cuerpo.has("systemInstruction"), cuerpo.toString());
            assertFalse(cuerpos.get(i).contains("thinkingConfig"), cuerpos.get(i));
            assertFalse(cuerpos.get(i).contains("responseMimeType"), cuerpos.get(i));
        }
        assertTrue(metodosYRutas.stream().noneMatch(p -> p.startsWith("GET ")), "Ya no se piden los metadatos");
        assertThat(salida.getOut() + salida.getErr()).contains("Gemini calentado en");
    }

    @Test
    void unaRespuestaCortadaPorElTopeNoEsUnaFalla(CapturedOutput salida) {
        // El servidor falso responde con MAX_TOKENS y una sola letra: es lo normal con 1 token.
        calentamiento(1).calentar();

        assertEquals(1, pedidos.get());
        assertThat(salida.getOut() + salida.getErr()).doesNotContain("No se pudo calentar");
    }

    // --- Fallas ---

    @Test
    void siFallaNoLanzaNadaYSoloRegistraLaClaseYElCodigo(CapturedOutput salida) {
        estado = 500;

        assertDoesNotThrow(() -> calentamiento(2).calentar());

        // Un error en el primer modelo no impide calentar el segundo.
        assertEquals(2, pedidos.get());
        String log = salida.getOut() + salida.getErr();
        assertTrue(log.contains("No se pudo calentar Gemini"), log);
        assertTrue(log.contains("código HTTP: 500"), log);
        assertFalse(log.contains("MENSAJE-SECRETO-DEL-SERVICIO"), log);
        assertFalse(log.contains(CLAVE), log);
    }

    @Test
    void sinModelosNoHaceNingunaLlamada() {
        CalentamientoDeGemini sinClave = new CalentamientoDeGemini(List.of(),
                new LimitadorDePedidos(Duration.ofSeconds(1)), MODELO);

        assertDoesNotThrow(sinClave::calentar);
        assertDoesNotThrow(sinClave::alIniciar);

        assertEquals(0, pedidos.get());
    }

    // --- Turno ---

    @Test
    void esperaSuTurnoDetrasDeUnChatYNoSeSuperponeConEl() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        CountDownLatch chatTieneElTurno = new CountDownLatch(1);
        CountDownLatch soltarElChat = new CountDownLatch(1);
        Thread chat = new Thread(() -> limitador.ejecutar(() -> {
            chatTieneElTurno.countDown();
            try {
                soltarElChat.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }));
        chat.start();
        assertTrue(chatTieneElTurno.await(10, TimeUnit.SECONDS));

        Thread calentando = new Thread(() -> calentamiento(2, limitador).calentar());
        calentando.start();
        Thread.sleep(700);
        assertEquals(0, pedidos.get(), "El calentamiento espera mientras el chat tiene el turno");

        soltarElChat.countDown();
        calentando.join(Duration.ofSeconds(20));
        chat.join(Duration.ofSeconds(5));
        assertEquals(2, pedidos.get());
    }

    // --- Arranque ---

    /** Con clave y proveedor {@code gemini} el bean existe; se apaga con la propiedad y no existe con Ollama. */
    @Test
    void elBeanSeApagaConLaPropiedadYNoExisteConOllama() {
        ApplicationContextRunner contexto = new ApplicationContextRunner()
                .withUserConfiguration(ConfiguracionIa.class, CalentamientoDeGemini.class)
                .withPropertyValues("alma.ia.timeout-llamada-ms=30000", "alma.ia.timeout-extraccion-ms=25000",
                        "alma.ia.espera-turno-ms=10000", "alma.ia.gemini.modelo=" + MODELO,
                        "alma.ia.gemini.base-url=http://127.0.0.1:" + servidor.getAddress().getPort(),
                        "spring.ai.ollama.base-url=http://localhost:11434",
                        "spring.ai.ollama.chat.options.model=modelo-de-prueba");

        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=" + CLAVE)
                .run(c -> assertThat(c).hasNotFailed().hasSingleBean(CalentamientoDeGemini.class));
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=" + CLAVE,
                "alma.ia.gemini.calentar=false")
                .run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(CalentamientoDeGemini.class));
        contexto.withPropertyValues("alma.ia.proveedor=ollama", "alma.ia.gemini.api-key=" + CLAVE)
                .run(c -> assertThat(c).hasNotFailed().doesNotHaveBean(CalentamientoDeGemini.class));
        // Sin clave: el bean existe pero no tiene modelos y no llama a la red.
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=").run(c -> {
            assertThat(c).hasNotFailed().hasSingleBean(CalentamientoDeGemini.class);
            c.getBean(CalentamientoDeGemini.class).alIniciar();
        });
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));
        assertEquals(0, pedidos.get(), "Ningún contexto de esta prueba debe llamar a la red al arrancar");
    }
}
