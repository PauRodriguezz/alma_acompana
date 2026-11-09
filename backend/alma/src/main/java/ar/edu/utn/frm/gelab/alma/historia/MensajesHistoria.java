package ar.edu.utn.frm.gelab.alma.historia;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;

/**
 * Textos que ve la persona en el módulo de historia de vida (contrato del grafo, sección 6). Los generales
 * (400, 401, 403, 404) son los de {@code common/MensajesError}; el texto de cada pregunta del guion está en
 * {@code historia/guion.json}.
 */
public final class MensajesHistoria {

    // Introducción de la próxima pregunta (5.2). La de "normal" depende del orden de la pregunta: orden % 3.
    public static final String INTRODUCCION_PRIMERA_VEZ = "Me gustaría conocerte mejor. Te voy a hacer preguntas "
            + "de a una sobre tu vida, y podés saltear las que quieras.";
    public static final String INTRODUCCION_DESPUES_DE_SALTEO = "Está bien, pasemos a otra.";
    public static final String[] INTRODUCCIONES_NORMALES = { "Sigamos con otra pregunta.",
            "Ahora me gustaría saber otra cosa.", "Vamos con la siguiente." };

    /** Cuando no queda ninguna pregunta pendiente (CU-03, camino alterno Nº4). */
    public static final String MENSAJE_FINAL = "Ya respondiste todas las preguntas que tengo por ahora. "
            + "¡Gracias por compartir tu historia conmigo!";

    // Validaciones (5.8).
    public static final String RESPUESTA_OBLIGATORIA = "Escribí una respuesta o elegí saltear la pregunta.";
    public static final String RESPUESTA_MUY_LARGA = "Tu respuesta es muy larga. Probá con una más corta.";

    // 409.
    public static final String PREGUNTA_YA_REGISTRADA = "Esa pregunta ya fue respondida.";
    /** Una repregunta que no aplica al recuerdo, ya no falta o ya se preguntó el máximo de veces (5.7). */
    public static final String REPREGUNTA_YA_NO_HACE_FALTA = "Esa pregunta ya no hace falta.";

    // Textos fijos de las repreguntas (5.7).
    public static final String REPREGUNTA_ANIO = "¿Te acordás en qué año fue, o cuántos años tenías?";
    public static final String REPREGUNTA_LUGARES = "¿Dónde pasó esto?";
    public static final String REPREGUNTA_PERSONAS = "¿Quién más estaba con vos en ese momento?";

    /** El texto fijo con el que se repregunta por un dato. */
    public static String repregunta(DatoFaltante dato) {
        return switch (dato) {
            case ANIO -> REPREGUNTA_ANIO;
            case LUGARES -> REPREGUNTA_LUGARES;
            case PERSONAS -> REPREGUNTA_PERSONAS;
        };
    }

    private MensajesHistoria() {
    }
}
