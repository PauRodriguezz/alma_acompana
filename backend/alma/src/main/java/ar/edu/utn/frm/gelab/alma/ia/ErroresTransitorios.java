package ar.edu.utn.frm.gelab.alma.ia;

import java.util.Set;

import org.springframework.ai.retry.TransientAiException;

import com.google.genai.errors.ApiException;

/**
 * Decide si un error de un proveedor es transitorio (contrato del grafo, 5.4, paso 1): de los que suelen pasar solos y
 * justifican repetir el pedido una vez. Solo mira el tipo y el código HTTP, nunca el mensaje (podría citar el
 * pedido). Los cortes por tiempo, la clave faltante y los 4xx que no son 429 no son transitorios.
 */
final class ErroresTransitorios {

    /** 429 (cuota por minuto), 500, 502, 503 (modelo sobrecargado) y 504. */
    private static final Set<Integer> CODIGOS_DE_GEMINI = Set.of(429, 500, 502, 503, 504);

    /** El límite evita un ciclo de causas. */
    private static final int PROFUNDIDAD_MAXIMA = 20;

    private ErroresTransitorios() {
    }

    /** Gemini: un {@link ApiException} con uno de los códigos de {@link #CODIGOS_DE_GEMINI}. */
    static boolean deGemini(Throwable error) {
        Throwable actual = error;
        for (int i = 0; i < PROFUNDIDAD_MAXIMA && actual != null; i++) {
            if (actual instanceof ApiException api) {
                return CODIGOS_DE_GEMINI.contains(api.code());
            }
            actual = actual.getCause() != actual ? actual.getCause() : null;
        }
        return false;
    }

    /** Ollama: Spring AI lanza {@link TransientAiException} para los 5xx. */
    static boolean deOllama(Throwable error) {
        Throwable actual = error;
        for (int i = 0; i < PROFUNDIDAD_MAXIMA && actual != null; i++) {
            if (actual instanceof TransientAiException) {
                return true;
            }
            actual = actual.getCause() != actual ? actual.getCause() : null;
        }
        return false;
    }
}
