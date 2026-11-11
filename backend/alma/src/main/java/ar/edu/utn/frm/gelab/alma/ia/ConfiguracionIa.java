package ar.edu.utn.frm.gelab.alma.ia;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente de Ollama con tope de tiempo y sin reintentos. La configuración automática de Spring AI usa un
 * {@code RetryTemplate} que reintenta hasta 10 veces (también los cortes por tiempo), lo que rompería el
 * máximo de 30 segundos; por eso estos beans la reemplazan ({@code @ConditionalOnMissingBean}).
 * La URL y el modelo salen de las propiedades que ya existen; los tiempos, de {@code chat.properties}.
 */
@Configuration
public class ConfiguracionIa {

    /** Servicio de Google; se usa cuando {@code alma.ia.gemini.base-url} está vacía. */
    static final String URL_BASE_GEMINI = "https://generativelanguage.googleapis.com";

    /**
     * Hace fallar el arranque con un mensaje claro si {@code IA_PROVEEDOR} no es {@code gemini} ni
     * {@code ollama}. Compara igual que {@code @ConditionalOnProperty} (sin importar mayúsculas y sin
     * recortar espacios): con otro valor no quedaría ninguna implementación activa. Es estático y corre
     * antes de crear cualquier bean; si no, {@code ChatService} fallaría primero con un error confuso.
     */
    @Bean
    static BeanFactoryPostProcessor validadorDeProveedorIa(Environment entorno) {
        return beanFactory -> {
            String proveedor = entorno.getProperty("alma.ia.proveedor", "gemini");
            if (!"gemini".equalsIgnoreCase(proveedor) && !"ollama".equalsIgnoreCase(proveedor)) {
                throw new IllegalStateException("IA_PROVEEDOR tiene un valor no válido: \"" + proveedor
                        + "\". Los valores posibles son \"gemini\" u \"ollama\" (ver docs/contrato-ia.md).");
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "alma.ia.proveedor", havingValue = "ollama")
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String urlBase,
            @Value("${alma.ia.timeout-llamada-ms}") long timeoutLlamadaMs) {
        return crearOllamaApi(urlBase, Duration.ofMillis(timeoutLlamadaMs));
    }

    @Bean
    @ConditionalOnProperty(name = "alma.ia.proveedor", havingValue = "ollama")
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.chat.options.model}") String modelo) {
        return crearChatModel(ollamaApi, modelo);
    }

    /**
     * Cliente de Gemini. Solo existe con proveedor {@code gemini} y con clave: sin clave la aplicación
     * arranca igual y {@link GeminiModeloConversacional} avisa y falla en cada pedido. La autoconfiguración
     * de Spring AI está apagada ({@code spring.ai.model.chat=none}).
     */
    @Bean
    @ConditionalOnProperty(name = "alma.ia.proveedor", havingValue = "gemini", matchIfMissing = true)
    @ConditionalOnExpression("!'${alma.ia.gemini.api-key:}'.isBlank()")
    public Client googleGenAiClient(@Value("${alma.ia.gemini.api-key}") String claveApi,
            @Value("${alma.ia.gemini.base-url:}") String urlBase,
            @Value("${alma.ia.timeout-llamada-ms}") long timeoutLlamadaMs) {
        return crearClienteGemini(claveApi, urlBase, Duration.ofMillis(timeoutLlamadaMs));
    }

    @Bean
    @ConditionalOnBean(Client.class)
    @ConditionalOnProperty(name = "alma.ia.proveedor", havingValue = "gemini", matchIfMissing = true)
    public GoogleGenAiChatModel googleGenAiChatModel(Client googleGenAiClient,
            @Value("${alma.ia.gemini.modelo}") String modelo) {
        return crearChatModelGemini(googleGenAiClient, modelo);
    }

    @Bean
    public LimitadorDePedidos limitadorDePedidos(@Value("${alma.ia.espera-turno-ms}") long esperaTurnoMs) {
        return new LimitadorDePedidos(Duration.ofMillis(esperaTurnoMs));
    }

    /** El tiempo de lectura corta toda la llamada: sin streaming, Ollama responde recién al terminar. */
    static OllamaApi crearOllamaApi(String urlBase, Duration timeout) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build());
        fabrica.setReadTimeout(timeout);
        return OllamaApi.builder()
                .baseUrl(urlBase)
                .restClientBuilder(RestClient.builder().requestFactory(fabrica))
                .responseErrorHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
                .build();
    }

    /**
     * Cliente del SDK de Google con tope de tiempo y un solo intento. El SDK trae su propio reintento
     * (5 intentos por defecto, también ante cortes por tiempo y 408/429/5xx): {@code attempts(1)} lo apaga.
     * {@code timeout} es el tope de toda la llamada (en OkHttp, {@code callTimeout}).
     * La clave se pasa siempre explícita, y {@code vertexAI(false)} evita que el SDK cambie de modo por una
     * variable de entorno ({@code GOOGLE_GENAI_USE_VERTEXAI}). Si la URL base está vacía se usa la de
     * Google, para que una variable de entorno perdida ({@code GOOGLE_GEMINI_BASE_URL}) no redirija la clave.
     * La clave viaja en la cabecera {@code x-goog-api-key}, no en la URL.
     */
    static Client crearClienteGemini(String claveApi, String urlBase, Duration timeout) {
        HttpOptions opcionesHttp = HttpOptions.builder()
                .baseUrl(urlBase != null && !urlBase.isBlank() ? urlBase.strip() : URL_BASE_GEMINI)
                .timeout(Math.toIntExact(timeout.toMillis()))
                .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                .build();
        return Client.builder()
                .apiKey(claveApi)
                .vertexAI(false)
                .httpOptions(opcionesHttp)
                .build();
    }

    /** Sin reintentos de Spring AI: un pedido fallido es un solo pedido. */
    static GoogleGenAiChatModel crearChatModelGemini(Client cliente, String modelo) {
        return GoogleGenAiChatModel.builder()
                .genAiClient(cliente)
                .options(GoogleGenAiChatOptions.builder().model(modelo).build())
                .retryTemplate(new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()))
                .build();
    }

    /** Sin reintentos: un pedido fallido es un solo pedido. */
    static OllamaChatModel crearChatModel(OllamaApi ollamaApi, String modelo) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaChatOptions.builder().model(modelo).build())
                .retryTemplate(new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()))
                .build();
    }
}
