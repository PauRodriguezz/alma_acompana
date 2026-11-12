package ar.edu.utn.frm.gelab.alma.ia;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.genai.errors.ApiException;

/**
 * Calienta Gemini al arrancar, para que el primer mensaje del chat no pague la primera generación (en la prueba real,
 * unos 3,5 s contra 0,8 s las siguientes; pedir solo los metadatos del modelo no alcanzó). Hace un
 * {@code generateContent} de 1 token con un texto fijo ("Hola"; nunca texto de una persona) con cada uno de los dos
 * modelos (el del chat y el de la extracción), que tienen clientes y conexiones distintos. Pasa por los mismos beans
 * de Spring AI que usa producción, así también se calienta el armado del pedido y la lectura de la respuesta.
 *
 * <p>Cada pedido va por {@link LimitadorDePedidos#ejecutarEnSegundoPlano}: cede el turno a cualquier chat y nunca se
 * superpone con otro pedido. Corre en un hilo aparte, después de {@code ApplicationReadyEvent}, y nunca rompe el
 * arranque. Sin clave no hace nada. Ante una falla registra solo la clase de la causa y el código HTTP, nunca el
 * mensaje (el del SDK puede citar el cuerpo de la respuesta); el contenido de la respuesta se descarta.
 *
 * <p>Costo: 2 pedidos por arranque con unos pocos tokens cada uno (cuentan para el límite de pedidos por minuto del
 * nivel gratuito). Se apaga con {@code alma.ia.gemini.calentar=false}. Límite: ayuda al primer mensaje después de
 * arrancar; si la demora es del lado de Google, solo la mueve al arranque.
 */
@Component
@ConditionalOnExpression("'${alma.ia.proveedor:gemini}'.equalsIgnoreCase('gemini') and ${alma.ia.gemini.calentar:true}")
public class CalentamientoDeGemini {

    private static final Logger log = LoggerFactory.getLogger(CalentamientoDeGemini.class);

    /** Texto fijo del pedido de calentamiento: no es de ninguna persona. */
    static final String TEXTO_DE_CALENTAMIENTO = "Hola";

    private final List<GoogleGenAiChatModel> modelos;
    private final LimitadorDePedidos limitador;
    private final String nombreDelModelo;

    @Autowired
    public CalentamientoDeGemini(
            @Qualifier(ConfiguracionIa.CHAT_MODEL_GEMINI_CHAT) ObjectProvider<GoogleGenAiChatModel> chat,
            @Qualifier(ConfiguracionIa.CHAT_MODEL_GEMINI_EXTRACCION) ObjectProvider<GoogleGenAiChatModel> extraccion,
            LimitadorDePedidos limitador, @Value("${alma.ia.gemini.modelo}") String nombreDelModelo) {
        this(List.of(chat, extraccion).stream().map(ObjectProvider::getIfAvailable).filter(Objects::nonNull)
                .toList(), limitador, nombreDelModelo);
    }

    CalentamientoDeGemini(List<GoogleGenAiChatModel> modelos, LimitadorDePedidos limitador, String nombreDelModelo) {
        this.modelos = modelos;
        this.limitador = limitador;
        this.nombreDelModelo = nombreDelModelo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alIniciar() {
        if (modelos.isEmpty()) {
            return; // sin clave: no hay modelos
        }
        Thread hilo = new Thread(this::calentar, "alma-calentamiento-gemini");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Un pedido de 1 token por modelo, en secuencia. Nunca lanza errores. */
    void calentar() {
        for (GoogleGenAiChatModel modelo : modelos) {
            long inicio = System.nanoTime();
            try {
                Prompt pedido = new Prompt(List.of(new UserMessage(TEXTO_DE_CALENTAMIENTO)),
                        GoogleGenAiChatOptions.builder().model(nombreDelModelo).maxOutputTokens(1).build());
                limitador.ejecutarEnSegundoPlano(() -> modelo.call(pedido));
                log.info("Gemini calentado en {} ms", milisDesde(inicio));
            } catch (RuntimeException e) {
                Integer codigo = codigoHttpDe(e);
                log.warn("No se pudo calentar Gemini tras {} ms (error: {}, código HTTP: {})", milisDesde(inicio),
                        causaRaiz(e).getClass().getSimpleName(), codigo != null ? codigo : "s/d");
            }
        }
    }

    private static Throwable causaRaiz(Throwable e) {
        Throwable actual = e;
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
