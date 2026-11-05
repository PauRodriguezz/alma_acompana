package ar.edu.utn.frm.gelab.alma.chat;

/**
 * Textos propios del módulo (contrato de chat, 5.1, 5.4, 5.9 y 6.4). Los textos generales viven en
 * common/MensajesError; las respuestas predefinidas, en chat.properties.
 */
public final class MensajesChat {

    // Saludo (5.1)
    public static final String BUEN_DIA = "Buen día";
    public static final String BUENAS_TARDES = "Buenas tardes";
    public static final String BUENAS_NOCHES = "Buenas noches";
    public static final String SALUDO_SIN_RECORDATORIOS = "Qué lindo verte.";
    public static final String SALUDO_UN_RECORDATORIO = "Qué lindo verte. Hoy tenés un recordatorio.";
    public static final String SALUDO_VARIOS_RECORDATORIOS = "Qué lindo verte. Hoy tenés recordatorios.";

    // Validaciones (5.9)
    public static final String MENSAJE_OBLIGATORIO = "Escribí un mensaje para Alma.";
    public static final String MENSAJE_MUY_LARGO = "Tu mensaje es muy largo. Probá con uno más corto.";

    // Error del modelo (6.4 y 7.2)
    public static final String TITULO_ALMA_NO_DISPONIBLE = "Alma no disponible";
    public static final String ALMA_NO_DISPONIBLE =
            "Alma no pudo responder en este momento. Intentá nuevamente en unos segundos.";

    // Contexto del modelo (5.4)
    public static final String ANIMO_SIN_REGISTRO = "No registró cómo se siente hoy.";
    public static final String SIN_RECORDATORIOS = "No tiene recordatorios para hoy.";
    public static final String SIN_INFORMACION = "No hay información.";

    private MensajesChat() {
    }
}
