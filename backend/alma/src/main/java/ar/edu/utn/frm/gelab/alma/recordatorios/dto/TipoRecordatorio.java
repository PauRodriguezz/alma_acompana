package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

/**
 * Discriminador de la API (no es un atributo del DC): distingue medicación de cita en las
 * respuestas y filtra la agenda.
 */
public enum TipoRecordatorio {
    MEDICACION,
    CITA
}
