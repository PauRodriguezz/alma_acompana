package ar.edu.utn.frm.gelab.alma.ia;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponseUsageMetadata;

/**
 * Habla con Gemini a través de Spring AI (docs/contrato-ia.md). Está activa con
 * {@code alma.ia.proveedor=gemini}, el valor por defecto. Aplica temperatura y tope de tokens, descarta las
 * partes de razonamiento, limpia la respuesta y la limita a un pedido a la vez. El tope de tiempo y la
 * ausencia de reintentos están en {@link ConfiguracionIa}.
 *
 * <p>Tiene dos clientes: el del chat (30 s, con prioridad) y el de la extracción de recuerdos (contrato del
 * grafo, 5.4): 25 s por llamada, temperatura y tokens propios, turno en segundo plano detrás del chat y salida
 * pedida como JSON ({@code responseMimeType}). Si la API rechaza ese campo, se reintenta sin él y se recuerda
 * para los pedidos siguientes.
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
    private static final String TIPO_JSON = "application/json";
    private static final String ETIQUETA_CHAT = "Gemini";
    private static final String ETIQUETA_EXTRACCION = "Gemini (extracción)";
    /** Tope de salida de la extracción si no se configura {@code alma.ia.gemini.extraccion-max-tokens}. */
    static final int TOPE_EXTRACCION_POR_DEFECTO = 1500;

    private final @Nullable GoogleGenAiChatModel chatModel;
    private final @Nullable GoogleGenAiChatModel chatModelExtraccion;
    private final LimitadorDePedidos limitador;
    private final String modelo;
    private final double temperatura;
    private final int maxTokens;
    private final int topeExtraccion;
    /** Pasa a {@code true} si la API rechazó {@code responseMimeType}: de ahí en más no se manda. */
    private volatile boolean sinTipoJson;

    /** El de siempre: la extracción usa el mismo cliente que el chat. */
    public GeminiModeloConversacional(@Nullable GoogleGenAiChatModel chatModel, LimitadorDePedidos limitador,
            String modelo, double temperatura, int maxTokens) {
        this(chatModel, chatModel, limitador, modelo, temperatura, maxTokens, TOPE_EXTRACCION_POR_DEFECTO);
    }

    /**
     * Los dos modelos son {@code null} si falta la clave: los beans no se crean (ver {@link ConfiguracionIa}).
     * {@code topeExtraccion}: piso del tope de salida de la extracción solo con Gemini. Si el modelo gasta
     * tokens pensando y esos cuentan dentro del tope, un tope de 600 podría cortar el JSON; se usa el mayor
     * entre este valor y el de las opciones de la llamada.
     */
    @Autowired
    public GeminiModeloConversacional(@Nullable GoogleGenAiChatModel chatModel,
            @Qualifier(ConfiguracionIa.CHAT_MODEL_GEMINI_EXTRACCION) @Nullable GoogleGenAiChatModel chatModelExtraccion,
            LimitadorDePedidos limitador, @Value("${alma.ia.gemini.modelo}") String modelo,
            @Value("${alma.ia.temperatura}") double temperatura, @Value("${alma.ia.max-tokens}") int maxTokens,
            @Value("${alma.ia.gemini.extraccion-max-tokens:" + TOPE_EXTRACCION_POR_DEFECTO + "}") int topeExtraccion) {
        this.chatModel = chatModel;
        this.chatModelExtraccion = chatModelExtraccion;
        this.limitador = limitador;
        this.modelo = modelo;
        this.temperatura = temperatura;
        this.maxTokens = maxTokens;
        this.topeExtraccion = topeExtraccion;
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
        Prompt prompt = armarPrompt(instrucciones, turnos, temperatura, maxTokens, false);
        return llamar(chatModel, prompt, false, ETIQUETA_CHAT);
    }

    /**
     * La extracción de recuerdos: cliente propio (25 s), temperatura y tokens de las opciones (con el piso
     * {@code topeExtraccion}), turno en segundo plano y salida JSON.
     */
    @Override
    public String responder(String instrucciones, List<TurnoConversacion> turnos, OpcionesDeLlamada opciones) {
        if (chatModelExtraccion == null) {
            throw new ModeloNoDisponibleException("Falta la clave de Gemini (GEMINI_API_KEY)");
        }
        int tope = Math.max(opciones.maxTokens(), topeExtraccion);
        boolean pedirJson = !sinTipoJson;
        try {
            return llamar(chatModelExtraccion,
                    armarPrompt(instrucciones, turnos, opciones.temperatura(), tope, pedirJson), true,
                    ETIQUETA_EXTRACCION);
        } catch (ModeloNoDisponibleException e) {
            if (!(pedirJson && rechazoElTipoJson(e))) {
                throw e;
            }
            // Algunos modelos no admiten responseMimeType: se recuerda y se repite sin ese campo.
            sinTipoJson = true;
            log.warn("Gemini rechazó responseMimeType (código HTTP 400): la extracción sigue sin pedir JSON "
                    + "desde la API y se repite este pedido sin ese campo");
            return llamar(chatModelExtraccion,
                    armarPrompt(instrucciones, turnos, opciones.temperatura(), tope, false), true,
                    ETIQUETA_EXTRACCION);
        }
    }

    private String llamar(GoogleGenAiChatModel cliente, Prompt prompt, boolean segundoPlano, String etiqueta) {
        long inicio = System.nanoTime();
        try {
            ChatResponse respuesta = segundoPlano
                    ? limitador.ejecutarEnSegundoPlano(() -> cliente.call(prompt))
                    : limitador.ejecutar(() -> cliente.call(prompt));
            registrarConsumo(respuesta, inicio, etiqueta);
            String texto = textoFinalDe(respuesta);
            if (texto.isEmpty()) {
                throw new ModeloNoDisponibleException("El modelo devolvió una respuesta vacía");
            }
            return texto;
        } catch (ModeloNoDisponibleException e) {
            registrarFalla(e, inicio, etiqueta);
            throw e;
        } catch (RuntimeException e) {
            registrarFalla(e, inicio, etiqueta);
            // No se pasa el mensaje original: el del SDK puede citar el cuerpo de la respuesta.
            throw new ModeloNoDisponibleException("Falló la llamada a Gemini: " + causaRaiz(e).getClass().getSimpleName(),
                    e, ErroresTransitorios.deGemini(e));
        }
    }

    /**
     * ¿La API rechazó {@code responseMimeType}? Un 400 cuyo mensaje nombra ese campo. El mensaje solo se
     * mira, nunca se registra.
     */
    private static boolean rechazoElTipoJson(Throwable e) {
        Throwable actual = e;
        for (int i = 0; i < 20 && actual != null; i++) {
            if (actual instanceof ApiException api && api.code() == 400) {
                String mensaje = api.message() == null ? "" : api.message().toLowerCase(Locale.ROOT);
                return mensaje.contains("mime") || mensaje.contains("response_mime") || mensaje.contains("responsemime");
            }
            actual = actual.getCause() != actual ? actual.getCause() : null;
        }
        return false;
    }

    /**
     * Gemini puede rechazar con 400 una conversación cuya primera entrada es del rol {@code model} (pasa
     * cuando el primer turno es la pregunta contextual del ánimo, de Alma). Por eso los turnos de Alma que
     * abren la conversación no se mandan como mensajes: su texto se agrega al final de las instrucciones, una
     * línea por turno y en orden, y la conversación se manda desde el primer turno de la persona. Si no hay
     * ningún turno de la persona, falla sin llamar a la red. Con turnos que empiezan en la persona (el caso
     * normal) las instrucciones y los mensajes salen tal cual.
     */
    private Prompt armarPrompt(String instrucciones, List<TurnoConversacion> turnos, double temperatura,
            int maxTokens, boolean pedirJson) {
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
        var opciones = GoogleGenAiChatOptions.builder()
                .model(modelo)
                .temperature(temperatura)
                .maxOutputTokens(maxTokens);
        if (pedirJson) {
            opciones.responseMimeType(TIPO_JSON);
        }
        return new Prompt(mensajes, opciones.build());
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

    private void registrarConsumo(ChatResponse respuesta, long inicio, String etiqueta) {
        Usage uso = respuesta != null && respuesta.getMetadata() != null ? respuesta.getMetadata().getUsage() : null;
        log.info("{} respondió en {} ms (tokens de entrada: {}, de salida: {}, de razonamiento: {})", etiqueta,
                milisDesde(inicio), uso != null ? uso.getPromptTokens() : "s/d",
                uso != null ? uso.getCompletionTokens() : "s/d", tokensDeRazonamiento(uso));
    }

    /** Los tokens que el modelo gastó pensando, si la API los informa; si no, "s/d". */
    private static Object tokensDeRazonamiento(Usage uso) {
        if (uso != null && uso.getNativeUsage() instanceof GenerateContentResponseUsageMetadata nativo) {
            return nativo.thoughtsTokenCount().map(Object.class::cast).orElse(0);
        }
        return "s/d";
    }

    /** Solo la clase de la causa raíz y el código HTTP si lo hay; nunca el mensaje. */
    private void registrarFalla(RuntimeException e, long inicio, String etiqueta) {
        Throwable raiz = causaRaiz(e);
        Integer codigoHttp = codigoHttpDe(e);
        log.warn("{} no pudo responder tras {} ms (error: {}, código HTTP: {})", etiqueta, milisDesde(inicio),
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
