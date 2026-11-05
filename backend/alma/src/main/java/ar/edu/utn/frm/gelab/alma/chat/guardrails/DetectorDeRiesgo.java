package ar.edu.utn.frm.gelab.alma.chat.guardrails;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.chat.ConfiguracionChat;

/**
 * Compara el mensaje nuevo con las dos listas de expresiones de {@code chat.properties} (contrato de chat,
 * 5.3). Malestar emocional grave tiene prioridad sobre urgencias médicas. Todo ocurre en memoria: no se
 * guarda ni se registra el texto (5.8).
 */
@Component
public class DetectorDeRiesgo {

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final List<Pattern> malestar;
    private final List<Pattern> urgencias;

    @Autowired
    public DetectorDeRiesgo(ConfiguracionChat configuracion) {
        this(configuracion.getGuardrails().getMalestar(), configuracion.getGuardrails().getUrgencias());
    }

    public DetectorDeRiesgo(List<String> expresionesMalestar, List<String> expresionesUrgencias) {
        this.malestar = compilar(expresionesMalestar);
        this.urgencias = compilar(expresionesUrgencias);
    }

    public Optional<TipoDeRiesgo> detectar(String mensaje) {
        String normalizado = normalizar(mensaje);
        if (coincide(malestar, normalizado)) {
            return Optional.of(TipoDeRiesgo.MALESTAR_GRAVE);
        }
        if (coincide(urgencias, normalizado)) {
            return Optional.of(TipoDeRiesgo.URGENCIA_MEDICA);
        }
        return Optional.empty();
    }

    /**
     * Minúsculas, sin tildes ni diéresis (la ñ pasa a n), todo lo que no sea letra o número pasa a un
     * espacio y se colapsan los espacios.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("");
        return NO_ALFANUMERICO.matcher(sinMarcas).replaceAll(" ").strip();
    }

    private static boolean coincide(List<Pattern> patrones, String normalizado) {
        return patrones.stream().anyMatch(patron -> patron.matcher(normalizado).find());
    }

    private static List<Pattern> compilar(List<String> expresiones) {
        return expresiones.stream().map(Pattern::compile).toList();
    }
}
