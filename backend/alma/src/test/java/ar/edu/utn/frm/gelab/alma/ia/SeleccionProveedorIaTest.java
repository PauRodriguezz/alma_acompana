package ar.edu.utn.frm.gelab.alma.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.google.genai.Client;

/**
 * Qué implementación de {@link ModeloConversacional} queda activa según {@code alma.ia.proveedor}
 * (contrato de IA, secciones 2 y 6). Usa solo las clases de {@code ia/}: no necesita base de datos.
 */
class SeleccionProveedorIaTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionIa.class, GeminiModeloConversacional.class,
                    OllamaModeloConversacional.class)
            .withPropertyValues(
                    "alma.ia.timeout-llamada-ms=30000",
                    "alma.ia.espera-turno-ms=10000",
                    "alma.ia.temperatura=0.6",
                    "alma.ia.max-tokens=400",
                    "alma.ia.gemini.modelo=gemini-de-prueba",
                    "spring.ai.ollama.base-url=http://localhost:11434",
                    "spring.ai.ollama.chat.options.model=modelo-de-prueba");

    @Test
    void conOllamaSoloExisteLaImplementacionDeOllama() {
        contexto.withPropertyValues("alma.ia.proveedor=ollama", "alma.ia.gemini.api-key=clave-de-prueba")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c).hasSingleBean(ModeloConversacional.class);
                    assertThat(c).hasSingleBean(OllamaModeloConversacional.class);
                    // Dos clientes (chat y extracción); ClientesDeIaTest comprueba cuál es cuál.
                    assertThat(c).getBeanNames(OllamaChatModel.class).hasSize(2);
                    assertThat(c).doesNotHaveBean(GeminiModeloConversacional.class);
                    assertThat(c).doesNotHaveBean(Client.class);
                    assertThat(c).doesNotHaveBean(GoogleGenAiChatModel.class);
                });
    }

    @Test
    void conGeminiSoloExisteLaImplementacionDeGemini() {
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=clave-de-prueba")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c).hasSingleBean(ModeloConversacional.class);
                    assertThat(c).hasSingleBean(GeminiModeloConversacional.class);
                    // Dos modelos (chat y extracción); ClientesDeIaTest comprueba cuál es cuál.
                    assertThat(c).getBeanNames(GoogleGenAiChatModel.class).hasSize(2);
                    assertThat(c).doesNotHaveBean(OllamaModeloConversacional.class);
                    assertThat(c).doesNotHaveBean(OllamaChatModel.class);
                });
    }

    @Test
    void sinProveedorConfiguradoElPorDefectoEsGemini() {
        contexto.withPropertyValues("alma.ia.gemini.api-key=clave-de-prueba").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c).hasSingleBean(GeminiModeloConversacional.class);
            assertThat(c).doesNotHaveBean(OllamaModeloConversacional.class);
        });
    }

    @Test
    void elValorDelProveedorNoDistingueMayusculas() {
        contexto.withPropertyValues("alma.ia.proveedor=GEMINI", "alma.ia.gemini.api-key=clave-de-prueba")
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c).hasSingleBean(GeminiModeloConversacional.class);
                });
    }

    @Test
    void conGeminiSinClaveElContextoArrancaYElModeloFallaEnCadaPedido() {
        contexto.withPropertyValues("alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c).hasSingleBean(GeminiModeloConversacional.class);
            assertThat(c).doesNotHaveBean(Client.class);
            assertThat(c).doesNotHaveBean(GoogleGenAiChatModel.class);
            ModeloConversacional modelo = c.getBean(ModeloConversacional.class);
            org.junit.jupiter.api.Assertions.assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder(
                    "I", java.util.List.of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Hola"))));
        });
    }

    @Test
    void unValorInvalidoHaceFallarElArranqueConUnMensajeClaro() {
        for (String invalido : new String[] { "openai", "", "ollama,gemini" }) {
            contexto.withPropertyValues("alma.ia.proveedor=" + invalido).run(c -> {
                assertThat(c).as("proveedor [" + invalido + "]").hasFailed();
                assertThat(c).getFailure().hasStackTraceContaining("IA_PROVEEDOR tiene un valor no válido")
                        .hasStackTraceContaining("\"gemini\" u \"ollama\"");
            });
        }
    }

    /** {@code withPropertyValues} recorta los espacios; esta prueba los inyecta sin recortar. */
    @Test
    void unValorConEspaciosSobrantesTambienHaceFallarElArranque() {
        contexto.withInitializer(c -> c.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("sin-recortar", Map.of("alma.ia.proveedor", "gemini "))))
                .run(c -> {
                    assertThat(c).hasFailed();
                    assertThat(c).getFailure().hasStackTraceContaining("IA_PROVEEDOR tiene un valor no válido");
                });
    }
}
