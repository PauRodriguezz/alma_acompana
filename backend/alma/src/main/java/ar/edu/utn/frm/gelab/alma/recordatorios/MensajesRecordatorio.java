package ar.edu.utn.frm.gelab.alma.recordatorios;

/**
 * Textos propios del módulo (contrato de recordatorios, secciones 5.8 y 6). Los textos generales
 * viven en common/MensajesError.
 */
public final class MensajesRecordatorio {

    // Medicación
    public static final String NOMBRE_OBLIGATORIO = "Escribí el nombre del medicamento.";
    public static final String NOMBRE_LARGO = "El nombre es demasiado largo.";
    public static final String DOSIS_OBLIGATORIA = "Indicá la dosis.";
    public static final String DOSIS_LARGA = "La dosis es demasiado larga.";
    public static final String HORA_OBLIGATORIA = "Elegí la hora.";
    public static final String FRECUENCIA_OBLIGATORIA = "Elegí cada cuánto la tomás.";
    public static final String FECHA_INICIO_PASADA = "La fecha de inicio no puede ser anterior a hoy.";
    public static final String DURACION_OBLIGATORIA = "Indicá cuántos días.";
    public static final String DURACION_FUERA_DE_RANGO = "Los días tienen que ser entre 1 y 365.";

    // Cita
    public static final String TITULO_OBLIGATORIO = "Escribí un título.";
    public static final String TITULO_LARGO = "El título es demasiado largo.";
    public static final String ESPECIALIDAD_OBLIGATORIA = "Escribí la especialidad.";
    public static final String ESPECIALIDAD_LARGA = "La especialidad es demasiado larga.";
    public static final String MEDICO_OBLIGATORIO = "Escribí el nombre del médico.";
    public static final String MEDICO_LARGO = "El nombre del médico es demasiado largo.";
    public static final String LUGAR_OBLIGATORIO = "Escribí el lugar.";
    public static final String LUGAR_LARGO = "El lugar es demasiado largo.";
    public static final String FECHA_HORA_OBLIGATORIA = "Elegí la fecha y la hora.";
    public static final String FECHA_HORA_PASADA = "La fecha y la hora tienen que ser posteriores a este momento.";
    public static final String INDICACIONES_LARGAS = "Las indicaciones son demasiado largas.";

    // Agenda
    public static final String RANGO_INVERTIDO = "La fecha final no puede ser anterior a la inicial.";
    public static final String RANGO_DEMASIADO_LARGO = "El rango no puede superar los 62 días.";

    // Estado de una notificación
    public static final String ESTADO_OBLIGATORIO = "Indicá el estado.";
    public static final String ESTADO_NO_CORRESPONDE = "Ese estado no corresponde a este recordatorio.";

    // Límites compartidos por los DTO y el servicio
    public static final int MAX_NOMBRE = 100;
    public static final int MAX_DOSIS = 100;
    public static final int MAX_TITULO = 100;
    public static final int MAX_ESPECIALIDAD = 100;
    public static final int MAX_MEDICO = 100;
    public static final int MAX_LUGAR = 150;
    public static final int MAX_INDICACIONES = 500;
    public static final int MIN_DURACION_DIAS = 1;
    public static final int MAX_DURACION_DIAS = 365;
    public static final int MAX_RANGO_AGENDA_DIAS = 62;

    private MensajesRecordatorio() {
    }
}
