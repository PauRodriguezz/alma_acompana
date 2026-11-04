package ar.edu.utn.frm.gelab.alma.bienestar;

import ar.edu.utn.frm.gelab.alma.bienestar.domain.NivelAnimo;

/**
 * Textos propios del módulo (contrato de ánimo, secciones 5.7, 6 y 9). Los textos generales viven
 * en common/MensajesError.
 */
public final class MensajesAnimo {

    public static final String NIVEL_OBLIGATORIO = "Elegí cómo te sentís.";

    // Preguntas contextuales: texto fijo por nivel, cálido, con una sola pregunta y sin género.
    public static final String PREGUNTA_MUY_MAL =
            "Gracias por contármelo. Estoy acá para acompañarte. ¿Querés charlar un ratito?";
    public static final String PREGUNTA_MAL =
            "Qué lástima que hoy no sea un buen día. Estoy acá para escucharte. ¿Querés contarme cómo viene tu día?";
    public static final String PREGUNTA_NEUTRO =
            "Gracias por contarme. A veces los días son así, tranquilos. ¿Qué estuviste haciendo hoy?";
    public static final String PREGUNTA_BIEN =
            "¡Qué bueno saberlo! ¿Qué fue lo mejor de tu día hasta ahora?";
    public static final String PREGUNTA_MUY_BIEN =
            "¡Qué alegría! Me encanta saberlo. ¿Qué hizo que hoy sea un día tan lindo?";

    private MensajesAnimo() {
    }

    public static String preguntaDe(NivelAnimo nivel) {
        return switch (nivel) {
            case MUY_MAL -> PREGUNTA_MUY_MAL;
            case MAL -> PREGUNTA_MAL;
            case NEUTRO -> PREGUNTA_NEUTRO;
            case BIEN -> PREGUNTA_BIEN;
            case MUY_BIEN -> PREGUNTA_MUY_BIEN;
        };
    }
}
