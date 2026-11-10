package ar.edu.utn.frm.gelab.alma.historia.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.historia.domain.TextoCortado;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaElPrompt;

/**
 * Las piezas de texto de {@code {progreso}} y {@code {recuerdos}} (contrato de historia de vida, 5.5): funciones
 * puras, sin base de datos. Nombre y mensaje se normalizan con la <b>misma</b> función,
 * {@link DetectorDeRiesgo#normalizar}.
 */
public final class TextoDeRecuerdos {

    private TextoDeRecuerdos() {
    }

    /**
     * ¿El mensaje (ya normalizado con {@link DetectorDeRiesgo#normalizar}) menciona el nombre como palabra
     * completa? El nombre se vuelve a normalizar con la misma función antes de comparar: el guardado conserva la
     * puntuación ("escuela n° 12") y el mensaje normalizado no la tiene ("escuela n 12"). Un nombre que queda
     * vacío no coincide con nada.
     */
    public static boolean menciona(String mensajeNormalizado, String nombre) {
        String buscado = DetectorDeRiesgo.normalizar(nombre);
        if (buscado.isEmpty() || mensajeNormalizado == null) {
            return false;
        }
        return (" " + mensajeNormalizado + " ").contains(" " + buscado + " ");
    }

    /** {@code Respondió 4, salteó 1, quedan 16.} (5.5). */
    public static String progreso(int respondidas, int salteadas, int pendientes) {
        return "Respondió " + respondidas + ", salteó " + salteadas + ", quedan " + pendientes + ".";
    }

    /**
     * Una línea por recuerdo: {@code - <título> (<año>): <descripción>. Con <persona> (<vínculo>), en <lugar>.}
     * Las partes que no existen se omiten. La descripción se corta a {@code descripcionMax} (con "…") y no se le
     * duplica el punto final. Todo va en una sola línea (los saltos del texto pasan a espacios). Con lugares y sin
     * personas queda {@code En <lugar>.}; varias personas o lugares se separan con ", ".
     */
    public static String linea(RecuerdoParaElPrompt recuerdo, int descripcionMax) {
        StringBuilder linea = new StringBuilder("- ").append(unaLinea(recuerdo.titulo()));
        if (recuerdo.anio() != null) {
            linea.append(" (").append(recuerdo.anio()).append(')');
        }
        String descripcion = TextoCortado.cortar(unaLinea(recuerdo.descripcion()), descripcionMax);
        String extras = extras(recuerdo);
        if (!descripcion.isEmpty()) {
            linea.append(": ").append(conPunto(descripcion));
        } else if (!extras.isEmpty()) {
            linea.append('.');
        }
        if (!extras.isEmpty()) {
            linea.append(' ').append(extras);
        }
        return linea.toString();
    }

    private static String extras(RecuerdoParaElPrompt recuerdo) {
        List<String> personas = recuerdo.personas().stream().map(TextoDeRecuerdos::persona).filter(Objects::nonNull)
                .toList();
        List<String> lugares = recuerdo.lugares().stream().map(TextoDeRecuerdos::unaLinea)
                .filter(l -> !l.isEmpty()).toList();
        List<String> partes = new ArrayList<>();
        if (!personas.isEmpty()) {
            partes.add("Con " + String.join(", ", personas));
        }
        if (!lugares.isEmpty()) {
            partes.add((personas.isEmpty() ? "En " : "en ") + String.join(", ", lugares));
        }
        return partes.isEmpty() ? "" : String.join(", ", partes) + ".";
    }

    /** "Elena (maestra)", "Elena" (sin vínculo) o "marido" (sin nombre); nulo si no hay ninguno de los dos. */
    private static String persona(RecuerdoParaElPrompt.Participante participante) {
        String nombre = unaLinea(participante.nombre());
        String vinculo = unaLinea(participante.vinculo());
        if (nombre.isEmpty()) {
            return vinculo.isEmpty() ? null : vinculo;
        }
        return vinculo.isEmpty() ? nombre : nombre + " (" + vinculo + ")";
    }

    private static String conPunto(String texto) {
        return texto.endsWith(".") || texto.endsWith(TextoCortado.PUNTOS) ? texto : texto + ".";
    }

    /** Espacios en los extremos fuera y cualquier salto de línea o grupo de espacios pasa a un espacio. */
    private static String unaLinea(String texto) {
        return texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
    }
}
