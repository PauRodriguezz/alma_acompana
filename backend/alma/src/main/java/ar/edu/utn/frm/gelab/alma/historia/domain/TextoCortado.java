package ar.edu.utn.frm.gelab.alma.historia.domain;

/**
 * Corte de textos largos (contrato de historia de vida, 7.3 y 5.5.2): se toman los primeros {@code max}
 * caracteres, se quitan los espacios del final y se agrega "…" si se cortó.
 */
public final class TextoCortado {

    public static final String PUNTOS = "…";

    private TextoCortado() {
    }

    /** El texto sin espacios en los extremos; si pasa de {@code max}, cortado con "…" al final. Nulo da vacío. */
    public static String cortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.strip();
        if (limpio.length() <= max) {
            return limpio;
        }
        return limpio.substring(0, max).stripTrailing() + PUNTOS;
    }
}
