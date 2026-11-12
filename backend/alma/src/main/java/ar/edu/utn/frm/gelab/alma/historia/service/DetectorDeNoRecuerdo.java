package ar.edu.utn.frm.gelab.alma.historia.service;

import java.util.regex.Pattern;

import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;

/**
 * Decide si una respuesta no tiene contenido: solo dice que la persona no recuerda, no sabe o no quiere contar
 * (contrato del grafo, 5.1 y 5.7). Una respuesta así no crea un recuerdo (ni completa uno): se trata como un salteo.
 * Función pura: no toca ninguna base ni el modelo, y no registra el texto.
 *
 * <p>Es deliberadamente estricta: el texto <b>completo</b> tiene que estar formado solo por frases de este tipo y
 * palabras de relleno, con al menos una frase. Si queda cualquier otra palabra ("no me acuerdo del año, pero iba con
 * Marta"), la respuesta tiene contenido y sigue su camino. Las listas son cerradas y se ajustan acá con los casos que
 * aparezcan; los tests de {@code DetectorDeNoRecuerdoTest} las protegen. Va después del detector de riesgo del
 * chat, que siempre tiene prioridad.
 */
public final class DetectorDeNoRecuerdo {

    /** Solo se evalúan respuestas cortas: una respuesta larga casi siempre cuenta algo. */
    static final int LARGO_MAXIMO = 60;

    /** Frases que dicen "no recuerdo", "no sé" o "no quiero contar". Van sin tildes y en minúscula (texto normalizado). */
    private static final String FRASE = String.join("|",
            "no (?:me )?(?:acuerdo|acorde|acordaba|recuerdo)",
            "no lo (?:recuerdo|se)",
            "no se",
            "no sabria decir(?:te|le|lo)?",
            "ni idea",
            "no tengo (?:ni )?idea",
            "no tengo (?:memoria|recuerdos?)",
            "me (?:olvide|olvido)",
            "se me (?:olvido|fue)",
            "lo olvide",
            "no me viene (?:nada|a la mente)",
            "nada(?: de nada)?",
            "(?:no quiero|prefiero no) (?:contar(?:lo|te)?|responder(?:lo)?|contestar(?:lo)?|decir(?:lo)?"
                    + "|hablar(?: de (?:eso|esto))?)");

    /** Palabras que acompañan una de esas frases sin agregar contenido. */
    private static final String RELLENO = String.join("|",
            "la verdad", "verdad", "sinceramente", "honestamente", "lamentablemente", "perdon", "perdoname",
            "disculpa", "disculpame", "lo siento", "che", "ay", "uy", "eh+", "m+", "hm+", "bueno", "mira", "ya",
            "ahora", "por ahora", "mucho", "muy", "bien", "casi", "realmente", "eso", "esto", "de eso", "de esto",
            "de todo eso", "de nada", "gracias", "alma", "que", "y");

    /** Cero o más rellenos, una frase y después más frases o rellenos. El texto ya viene normalizado (espacios simples). */
    private static final Pattern SIN_CONTENIDO = Pattern.compile(
            "^(?:(?:" + RELLENO + ") )*(?:" + FRASE + ")(?: (?:" + RELLENO + "|" + FRASE + "))*$");

    private DetectorDeNoRecuerdo() {
    }

    /** ¿La respuesta solo dice que no recuerda, no sabe o no quiere contar? Un texto nulo o vacío no cuenta. */
    public static boolean esSinContenido(String texto) {
        String normalizado = DetectorDeRiesgo.normalizar(texto);
        return !normalizado.isEmpty() && normalizado.length() <= LARGO_MAXIMO
                && SIN_CONTENIDO.matcher(normalizado).matches();
    }
}
