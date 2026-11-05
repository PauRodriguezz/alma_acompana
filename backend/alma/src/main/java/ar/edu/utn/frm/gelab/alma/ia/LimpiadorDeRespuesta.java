package ar.edu.utn.frm.gelab.alma.ia;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deja solo el texto final de la respuesta del modelo: quita el bloque de razonamiento (entre
 * {@code <think>} y {@code </think>}) y los espacios de los extremos (contrato de chat, 5.6).
 */
public final class LimpiadorDeRespuesta {

    private static final String CIERRE = "</think>";
    private static final Pattern BLOQUE_SIN_CERRAR = Pattern.compile("<think>.*$",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private LimpiadorDeRespuesta() {
    }

    /** Devuelve el texto limpio, o cadena vacía si no quedó nada (quien lo llama lo trata como falla). */
    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto;
        // Nos quedamos con lo que viene después del último cierre (también si el modelo omitió la apertura).
        int cierre = limpio.toLowerCase(Locale.ROOT).lastIndexOf(CIERRE);
        if (cierre >= 0) {
            limpio = limpio.substring(cierre + CIERRE.length());
        }
        // Un bloque que nunca se cierra no es una respuesta.
        limpio = BLOQUE_SIN_CERRAR.matcher(limpio).replaceAll("");
        return limpio.strip();
    }
}
