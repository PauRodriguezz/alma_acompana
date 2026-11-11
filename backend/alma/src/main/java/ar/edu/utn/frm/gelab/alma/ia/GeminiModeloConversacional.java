package ar.edu.utn.frm.gelab.alma.ia;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.genai.errors.ApiException;

/**
 * Habla con Gemini a través de Spring AI (docs/contrato-ia.md). Está activa con
 * {@code alma.ia.proveedor=gemini}, el valor por defecto. Aplica temperatura y tope de tokens, descarta las
 * partes de razonamiento, limpia la respuesta y la limita a un pedido a la vez. El tope de tiempo y la
 * ausencia de reintentos están en {@link ConfiguracionIa}.
 *
 * <p>Sin clave ({@code GEMINI_API_KEY}) la aplicación arranca igual: no hay modelo, se avisa en el log y
 * cada pedido falla con {@link ModeloNoDisponibleException} sin hacer ninguna llamada de red.
 *
 * <p>Privacidad (contrato del chat, 5.8, y contrato de IA, sección 5): el log registra duración, tokens y
 * tipo de error (clase de la causa raíz y código HTTP); nunca textos, prompt, clave ni mensajes de
 * excepciones (el del SDK puede incluir el cuerpo de la respuesta).
 */
@Service
@ConditionalOnProperty(name = "alma.ia.proveedor", havingValue = "gemini", matchIfMissing = true)
public class GeminiModeloConversacional implements ModeloConversacional {

    private static final Logger log = LoggerFactory.getLogger(GeminiModeloConversacional.class);

    private static final String PARTE_DE_RAZONAMIENTO = "isThought";
    private static final String CORTADA_POR_TOPE = "MAX_TOKENS";
    private static final String PREFIJO_TURNO_PREVIO_DE_ALMA = "Lo último que le dijiste a la persona antes de este mensaje: ";

    private final @Nullable GoogleGenAiChatModel chatModel;
    private final LimitadorDePedidos limitador;
    private final String modelo;
    private final double temperatura;
    private final int maxTokens;

    /** {@code chatModel} es {@code null} si falta la clave: el bean no se crea (ver {@link ConfiguracionIa}). */
    public GeminiModeloConversacional(@Nullable GoogleGenAiChatModel chatModel, LimitadorDePedidos limitador,
            @Value("${alma.ia.gemini.modelo}") String modelo,
            @Value("${alma.ia.temperatura}") double temperatura,
            @Value("${alma.ia.max-tokens}") int maxTokens) {
        this.chatModel = chatModel;
        this.limitador = limitador;
        this.modelo = modelo;
        this.temperatura = temperatura;
        this.maxTokens = maxTokens;
        if (chatModel == null) {
            log.warn("Falta GEMINI_API_KEY: el chat no podrá responder hasta que se configure "
                    + "(o se use IA_PROVEEDOR=ollama). El resto de la aplicación funciona igual.");
        }
    }

    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos) {
        if (chatModel == null) {
            throw new ModeloNoDisponibleException("Falta la clave de Gemini (GEMINI_API_KEY)");
        }
        Prompt prompt = armarPrompt(instrucciones, turnos);
        long inicio = System.nanoTime();
        try {
            ChatResponse respuesta = limitador.ejecutar(() -> chatModel.call(prompt));
            registrarConsumo(respuesta, inicio);
            String texto = textoFinalDe(respuesta);
            if (texto.isEmpty()) {
                throw new ModeloNoDisponibleException("El modelo devolvió una respuesta vacía");
            }
            return texto;
        } catch (ModeloNoDisponibleException e) {
            registrarFalla(e, inicio);
            throw e;
        } catch (RuntimeException e) {
            registrarFalla(e, inicio);
            // No se pasa el mensaje original: el del SDK puede citar el cuerpo de la respuesta.
            throw new ModeloNoDisponibleException("Falló la llamada a Gemini: " + causaRaiz(e).getClass().getSimpleName(),
                    e);
        }
    }

    /**
     * Gemini puede rechazar con 400 una conversación cuya primera entrada es del rol {@code model} (pasa
     * cuando el primer turno es la pregunta contextual del ánimo, de Alma). Por eso los turnos de Alma que
     * abren la conversación no se mandan como mensajes: su texto se agrega al final de las instrucciones, una
     * línea por turno y en orden, y la conversación se manda desde el primer turno de la persona. Si no hay
     * ningún turno de la persona, falla sin llamar a la red. Con turnos que empiezan en la persona (el caso
     * normal) las instrucciones y los mensajes salen tal cual.
     */
    private Prompt armarPrompt(String instrucciones, List<TurnoConversacion> turnos) {
        int primeroDeLaPersona = 0;
        while (primeroDeLaPersona < turnos.size() && turnos.get(primeroDeLaPersona).rol() == TurnoConversacion.Rol.ALMA) {
            primeroDeLaPersona++;
        }
        if (primeroDeLaPersona == turnos.size()) {
            log.warn("Gemini: la conversación no tiene ningún turno de la persona; no se llama al modelo");
            throw new ModeloNoDisponibleException("La conversación no tiene ningún turno de la persona");
        }
        String instruccionesFinales = instrucciones;
        if (primeroDeLaPersona > 0) {
            StringBuilder ampliadas = new StringBuilder(instrucciones);
            if (!instrucciones.endsWith("\n")) {
                ampliadas.append('\n');
            }
            for (int i = 0; i < primeroDeLaPersona; i++) {
                ampliadas.append(PREFIJO_TURNO_PREVIO_DE_ALMA).append(turnos.get(i).texto()).append('\n');
            }
            instruccionesFinales = ampliadas.toString().stripTrailing();
        }
        List<Message> mensajes = new ArrayList<>();
        mensajes.add(new SystemMessage(instruccionesFinales));
        for (TurnoConversacion turno : turnos.subList(primeroDeLaPersona, turnos.size())) {
            mensajes.add(turno.rol() == TurnoConversacion.Rol.ALMA
                    ? new AssistantMessage(turno.texto())
                    : new UserMessage(turno.texto()));
        }
        // No se configura razonamiento (sin ThinkingConfig): contrato de IA, sección 2.
        GoogleGenAiChatOptions opciones = GoogleGenAiChatOptions.builder()
                .model(modelo)
                .temperature(temperatura)
                .maxOutputTokens(maxTokens)
                .build();
        return new Prompt(mensajes, opciones);
    }

    /**
     * Spring AI devuelve una {@code Generation} por parte de la respuesta y marca las de razonamiento con
     * {@code isThought}. {@code getResult()} trae solo la primera (podría ser razonamiento), así que se
     * recorren todas, se descartan las de razonamiento y se une el texto de las demás.
     */
    private static String textoFinalDe(ChatResponse respuesta) {
        if (respuesta == null || respuesta.getResults() == null) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        boolean cortadaPorTope = false;
        for (Generation generacion : respuesta.getResults()) {
            if (generacion == null || generacion.getOutput() == null) {
                continue;
            }
            if (generacion.getMetadata() != null
                    && CORTADA_POR_TOPE.equalsIgnoreCase(generacion.getMetadata().getFinishReason())) {
                cortadaPorTope = true;
            }
            if (Boolean.TRUE.equals(generacion.getOutput().getMetadata().get(PARTE_DE_RAZONAMIENTO))) {
                continue;
            }
            String parte = generacion.getOutput().getText();
            if (parte != null) {
                texto.append(parte);
            }
        }
        String limpio = LimpiadorDeRespuesta.limpiar(texto.toString());
        if (cortadaPorTope && !limpio.isEmpty()) {
            // Se usa el texto recibido (igual que con Ollama); el aviso no lleva el texto.
            log.warn("La respuesta de Gemini se cortó por el tope de tokens de salida (MAX_TOKENS)");
        }
        return limpio;
    }

    private void registrarConsumo(ChatResponse respuesta, long inicio) {
        Usage uso = respuesta != null && respuesta.getMetadata() != null ? respuesta.getMetadata().getUsage() : null;
        log.info("Gemini respondió en {} ms (tokens de entrada: {}, de salida: {})", milisDesde(inicio),
                uso != null ? uso.getPromptTokens() : "s/d", uso != null ? uso.getCompletionTokens() : "s/d");
    }

    /** Solo la clase de la causa raíz y el código HTTP si lo hay; nunca el mensaje. */
    private void registrarFalla(RuntimeException e, long inicio) {
        Throwable raiz = causaRaiz(e);
        Integer codigoHttp = codigoHttpDe(e);
        log.warn("Gemini no pudo responder tras {} ms (error: {}, código HTTP: {})", milisDesde(inicio),
                raiz.getClass().getSimpleName(), codigoHttp != null ? codigoHttp : "s/d");
    }

    private static Throwable causaRaiz(Throwable e) {
        Throwable actual = e;
        // El límite evita un ciclo de causas.
        for (int i = 0; i < 20 && actual.getCause() != null && actual.getCause() != actual; i++) {
            actual = actual.getCause();
        }
        return actual;
    }

    private static Integer codigoHttpDe(Throwable e) {
        Throwable actual = e;
        for (int i = 0; i < 20 && actual != null; i++) {
            if (actual instanceof ApiException api) {
                return api.code();
            }
            actual = actual.getCause() != actual ? actual.getCause() : null;
        }
        return null;
    }

    private static long milisDesde(long inicioNanos) {
        return (System.nanoTime() - inicioNanos) / 1_000_000;
    }
}
