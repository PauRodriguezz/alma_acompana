package ar.edu.utn.frm.gelab.alma.common;

/**
 * Textos de validación que ve la persona operadora al cargar ejercicios y microcápsulas desde el panel
 * (contrato del panel de operador). Los largos máximos viven en cada servicio y entran como parámetro.
 */
public final class MensajesPanel {

    // --- Comunes a ejercicios y microcápsulas ---
    public static final String ELEGI_CATEGORIA = "Elegí una categoría.";
    public static final String IMAGEN_INVALIDA = "La imagen no es válida. Volvé a elegirla.";

    // --- Ejercicios ---
    public static final String EJERCICIO_CAMPOS_REQUERIDOS = "Completá todos los campos requeridos del ejercicio.";
    public static final String EJERCICIO_OPCIONES_MULTIPLE = "Cargá de 3 a 4 opciones y marcá una sola como correcta.";
    public static final String EJERCICIO_OPCIONES_ORDENAR =
            "Cargá de 3 a 5 pasos y asignale a cada uno una posición distinta.";
    public static final String EJERCICIO_ELEGI_FORMATO = "Elegí un formato.";
    public static final String EJERCICIO_INGRESA_CONSIGNA = "Ingresá la consigna.";
    public static final String EJERCICIO_ELEGI_NIVEL = "Elegí un nivel de dificultad.";
    public static final String EJERCICIO_INGRESA_TEXTO_A_MEMORIZAR = "Ingresá el texto a memorizar.";
    public static final String EJERCICIO_INGRESA_TEXTO_OPCION = "Ingresá el texto de la opción.";

    // --- Microcápsulas ---
    public static final String MICROCAPSULA_CAMPOS_REQUERIDOS =
            "Completá todos los campos requeridos de la microcápsula.";
    public static final String MICROCAPSULA_FORMATO_NO_PERMITIDO = "El texto tiene un formato que no se puede usar. "
            + "Usá solo título, subtítulo, negrita, cursiva, listas y destacado.";
    public static final String MICROCAPSULA_INGRESA_TITULO = "Ingresá el título.";
    public static final String MICROCAPSULA_INGRESA_TEXTO = "Ingresá el texto.";
    public static final String MICROCAPSULA_INGRESA_FECHA = "Ingresá la fecha de publicación.";
    public static final String MICROCAPSULA_FECHA_FUTURA = "La fecha de publicación no puede ser futura.";

    private MensajesPanel() {
    }

    public static String consignaLarga(int maximo) {
        return "La consigna puede tener hasta " + maximo + " caracteres.";
    }

    public static String explicacionLarga(int maximo) {
        return "La explicación puede tener hasta " + maximo + " caracteres.";
    }

    public static String textoAMemorizarLargo(int maximo) {
        return "El texto a memorizar puede tener hasta " + maximo + " caracteres.";
    }

    public static String textoDeOpcionLargo(int maximo) {
        return "El texto de la opción puede tener hasta " + maximo + " caracteres.";
    }

    public static String tituloLargo(int maximo) {
        return "El título puede tener hasta " + maximo + " caracteres.";
    }

    public static String textoLargo(int maximo) {
        return "El texto puede tener hasta " + maximo + " caracteres.";
    }
}
