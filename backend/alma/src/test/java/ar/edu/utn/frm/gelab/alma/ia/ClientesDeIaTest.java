package ar.edu.utn.frm.gelab.alma.ia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.genai.Client;

/**
 * Arranque con los dos clientes de cada proveedor (chat y extracción): que existan solo con su proveedor, que
 * no haya ambigüedad al inyectar y que cada modelo se arme sobre el cliente que le corresponde. Sin base de
 * datos: usa solo las clases de {@code ia/}.
 */
class ClientesDeIaTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionIa.class, GeminiModeloConversacional.class,
                    OllamaModeloConversacional.class)
            .withPropertyValues(
                    "alma.ia.timeout-llamada-ms=30000",
                    "alma.ia.timeout-extraccion-ms=25000",
                    "alma.ia.espera-turno-ms=10000",
                    "alma.ia.temperatura=0.6",
                    "alma.ia.max-tokens=400",
                    "alma.ia.gemini.modelo=gemini-de-prueba",
                    "spring.ai.ollama.base-url=http://localhost:11434",
                    "spring.ai.ollama.chat.options.model=modelo-de-prueba");

    @Test
    void conGeminiHayDosClientesYDosModelosYElDelChatEsElPrimario() {
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=clave-de-prueba").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBeansOfType(Client.class)).containsOnlyKeys("googleGenAiClient",
                    "googleGenAiClientExtraccion");
            assertThat(c.getBeansOfType(GoogleGenAiChatModel.class)).containsOnlyKeys("googleGenAiChatModel",
                    "googleGenAiChatModelExtraccion");
            // Sin calificador, gana el del chat.
            assertThat(c.getBean(Client.class)).isSameAs(c.getBean("googleGenAiClient"));
            assertThat(c.getBean(GoogleGenAiChatModel.class)).isSameAs(c.getBean("googleGenAiChatModel"));
            // Cada modelo sobre su cliente (el chat con 30 s y la extracción con 25 s, distintos objetos).
            assertThat(c.getBean("googleGenAiClient")).isNotSameAs(c.getBean("googleGenAiClientExtraccion"));
            // Y la implementación recibe cada uno en su lugar.
            GeminiModeloConversacional gemini = c.getBean(GeminiModeloConversacional.class);
            assertThat(ReflectionTestUtils.getField(gemini, "chatModel"))
                    .isSameAs(c.getBean("googleGenAiChatModel"));
            assertThat(ReflectionTestUtils.getField(gemini, "chatModelExtraccion"))
                    .isSameAs(c.getBean("googleGenAiChatModelExtraccion"));
        });
    }

    @Test
    void conGeminiNoExisteNingunBeanDeOllamaNiLosDeExtraccionDeOllama() {
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=clave-de-prueba").run(c -> {
            assertThat(c).doesNotHaveBean(OllamaChatModel.class);
            assertThat(c).doesNotHaveBean(OllamaApi.class);
            assertThat(c).doesNotHaveBean("ollamaApiExtraccion");
            assertThat(c).doesNotHaveBean("ollamaChatModelExtraccion");
        });
    }

    @Test
    void conOllamaHayDosClientesDeOllamaYNingunoDeGemini() {
        contexto.withPropertyValues("alma.ia.proveedor=ollama", "alma.ia.gemini.api-key=clave-de-prueba").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBeansOfType(OllamaChatModel.class)).containsOnlyKeys("ollamaChatModel",
                    "ollamaChatModelExtraccion");
            assertThat(c.getBean(OllamaChatModel.class)).isSameAs(c.getBean("ollamaChatModel"));
            OllamaModeloConversacional ollama = c.getBean(OllamaModeloConversacional.class);
            assertThat(ReflectionTestUtils.getField(ollama, "chatModelExtraccion"))
                    .isSameAs(c.getBean("ollamaChatModelExtraccion"));
            assertThat(c).doesNotHaveBean(Client.class);
            assertThat(c).doesNotHaveBean(GoogleGenAiChatModel.class);
            assertThat(c).doesNotHaveBean(GeminiModeloConversacional.class);
        });
    }

    @Test
    void conGeminiSinClaveArrancaSinNingunClienteNiModeloYAvisaEnCadaPedido() {
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c).doesNotHaveBean(Client.class);
            assertThat(c).doesNotHaveBean(GoogleGenAiChatModel.class);
            assertThat(c).hasSingleBean(GeminiModeloConversacional.class);
        });
    }
}
