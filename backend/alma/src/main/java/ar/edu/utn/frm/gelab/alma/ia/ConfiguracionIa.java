package ar.edu.utn.frm.gelab.alma.ia;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

    static final String API_EXTRACCION = "ollamaApiExtraccion";
    static final String CHAT_MODEL_EXTRACCION = "ollamaChatModelExtraccion";

    @Bean
    @Primary
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String urlBase,
            @Value("${alma.ia.timeout-llamada-ms}") long timeoutLlamadaMs) {
        return crearOllamaApi(urlBase, Duration.ofMillis(timeoutLlamadaMs));
    }

    @Bean
    @Primary
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.chat.options.model}") String modelo) {
        return crearChatModel(ollamaApi, modelo);
    }

    /**
     * Segundo cliente, para la extracción de recuerdos (contrato del grafo, 5.4): mismo servidor y mismo modelo,
     * con su propio tope de 25 s por llamada. Los dos primeros son {@code @Primary}: el chat y Spring AI siguen
     * recibiendo los de siempre.
     */
    @Bean(API_EXTRACCION)
    public OllamaApi ollamaApiExtraccion(@Value("${spring.ai.ollama.base-url}") String urlBase,
            @Value("${alma.ia.timeout-extraccion-ms:25000}") long timeoutMs) {
        return crearOllamaApi(urlBase, Duration.ofMillis(timeoutMs));
    }

    @Bean(CHAT_MODEL_EXTRACCION)
    public OllamaChatModel ollamaChatModelExtraccion(@Qualifier(API_EXTRACCION) OllamaApi ollamaApi,
            @Value("${spring.ai.ollama.chat.options.model}") String modelo) {
        return crearChatModel(ollamaApi, modelo);
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

    /** Sin reintentos: un pedido fallido es un solo pedido. */
    static OllamaChatModel crearChatModel(OllamaApi ollamaApi, String modelo) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(OllamaChatOptions.builder().model(modelo).build())
                .retryTemplate(new RetryTemplate(RetryPolicy.builder().maxRetries(0).build()))
                .build();
    }
}
