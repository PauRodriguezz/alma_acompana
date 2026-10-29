package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

/**
 * Un recordatorio tal como viaja en la API: su forma depende del tipo (contrato, sección 6).
 */
public sealed interface RecordatorioResponse permits MedicacionResponse, CitaResponse {

    Long id();

    TipoRecordatorio tipo();
}
