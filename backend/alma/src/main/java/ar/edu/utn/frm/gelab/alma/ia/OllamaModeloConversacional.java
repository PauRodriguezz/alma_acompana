package ar.edu.utn.frm.gelab.alma.ia;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Habla con Ollama a través de Spring AI (contrato de chat, 5.6 y 5.7). Desactiva el razonamiento en el
 * pedido, aplica temperatura y tope de tokens, limpia la respuesta y la limita a un pedido a la vez. El
 * tope de tiempo y la ausencia de reintentos están en {@link ConfiguracionIa}.
 *
 * <p>Privacidad (5.8): el log registra duración, tokens y tipo de error; nunca el texto del prompt, de los
 * mensajes ni de las respuestas, ni el mensaje de las excepciones (que podría citarlos).
 */
@Service
public class OllamaModeloConversacional implements ModeloConversacional {

    private static final Logger log = LoggerFactory.getLogger(OllamaModeloConversacional.class);

    private final OllamaChatModel chatModel;
    private final OllamaChatModel chatModelExtraccion;
    private final LimitadorDePedidos limitador;
    private final String modelo;
    private final double temperatura;
    private final int maxTokens;

    /** El de siempre: la extracción usa el mismo cliente que el chat. */
    public OllamaModeloConversacional(OllamaChatModel chatModel, LimitadorDePedidos limitador, String modelo,
            double temperatura, int maxTokens) {
        this(chatModel, chatModel, limitador, modelo, temperatura, maxTokens);
    }

    @Autowired
    public OllamaModeloConversacional(OllamaChatModel chatModel,
            @Qualifier(ConfiguracionIa.CHAT_MODEL_EXTRACCION) OllamaChatModel chatModelExtraccion,
            LimitadorDePedidos limitador, @Value("${spring.ai.ollama.chat.options.model}") String modelo,
            @Value("${alma.ia.temperatura}") double temperatura, @Value("${alma.ia.max-tokens}") int maxTokens) {
        this.chatModel = chatModel;
        this.chatModelExtraccion = chatModelExtraccion;
        this.limitador = limitador;
        this.modelo = modelo;
        this.temperatura = temperatura;
        this.maxTokens = maxTokens;
    }

    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos) {
        return llamar(chatModel, armarPrompt(instrucciones, turnos, temperatura, maxTokens));
    }

    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos, OpcionesDeLlamada opciones) {
        return llamar(chatModelExtraccion,
                armarPrompt(instrucciones, turnos, opciones.temperatura(), opciones.maxTokens()));
    }

    private String llamar(OllamaChatModel cliente, Prompt prompt) {
        long inicio = System.nanoTime();
        try {
            ChatResponse respuesta = limitador.ejecutar(() -> cliente.call(prompt));
            registrarConsumo(respuesta, inicio);
            String texto = textoDe(respuesta);
            if (texto.isEmpty()) {
                throw new ModeloNoDisponibleException("El modelo devolvió una respuesta vacía");
            }
            return texto;
        } catch (ModeloNoDisponibleException e) {
            registrarFalla(e, inicio);
            throw e;
        } catch (RuntimeException e) {
            registrarFalla(e, inicio);
            // No se pasa el mensaje original: podría citar el pedido.
            throw new ModeloNoDisponibleException("Falló la llamada al modelo: " + e.getClass().getSimpleName(), e);
        }
    }

    private Prompt armarPrompt(String instrucciones, List<TurnoConversacion> turnos, double temperatura,
            int maxTokens) {
        List<Message> mensajes = new ArrayList<>();
        mensajes.add(new SystemMessage(instrucciones));
        for (TurnoConversacion turno : turnos) {
            mensajes.add(turno.rol() == TurnoConversacion.Rol.ALMA
                    ? new AssistantMessage(turno.texto())
                    : new UserMessage(turno.texto()));
        }
        OllamaChatOptions opciones = OllamaChatOptions.builder()
                .model(modelo)
                .temperature(temperatura)
                .numPredict(maxTokens)
                .disableThinking()
                .build();
        return new Prompt(mensajes, opciones);
    }

    private static String textoDe(ChatResponse respuesta) {
        if (respuesta == null || respuesta.getResult() == null || respuesta.getResult().getOutput() == null) {
            return "";
        }
        // getText() trae solo el contenido; el razonamiento viaja aparte (metadato "thinking").
        return LimpiadorDeRespuesta.limpiar(respuesta.getResult().getOutput().getText());
    }

    private void registrarConsumo(ChatResponse respuesta, long inicio) {
        Usage uso = respuesta != null && respuesta.getMetadata() != null ? respuesta.getMetadata().getUsage() : null;
        log.info("Modelo respondió en {} ms (tokens de entrada: {}, de salida: {})", milisDesde(inicio),
                uso != null ? uso.getPromptTokens() : "s/d", uso != null ? uso.getCompletionTokens() : "s/d");
    }

    private void registrarFalla(RuntimeException e, long inicio) {
        log.warn("El modelo no pudo responder tras {} ms ({})", milisDesde(inicio), e.getClass().getSimpleName());
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
