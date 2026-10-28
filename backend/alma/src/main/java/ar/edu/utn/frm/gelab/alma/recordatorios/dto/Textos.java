package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

/**
 * Normaliza los textos que manda la persona (contrato, sección 3): sin espacios al inicio y al final.
 */
final class Textos {

    /** Recorta el texto. Un texto que queda vacío sigue vacío, para que falle como obligatorio. */
    static String recortar(String texto) {
        return texto == null ? null : texto.strip();
    }

    /** Como recortar, pero un texto opcional vacío pasa a null. */
    static String recortarOpcional(String texto) {
        String recortado = recortar(texto);
        return recortado == null || recortado.isEmpty() ? null : recortado;
    }

    private Textos() {
    }
}
