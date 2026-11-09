package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * {@code nombreNormalizado} de personas y lugares (contrato del grafo, 5.6): minúsculas, sin tildes ni
 * diéresis (la {@code ñ} pasa a {@code n}) y espacios colapsados.
 */
public final class NombreNormalizado {

    private NombreNormalizado() {
    }

    public static String de(String nombre) {
        if (nombre == null) {
            return null;
        }
        String sinMarcas = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinMarcas.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
