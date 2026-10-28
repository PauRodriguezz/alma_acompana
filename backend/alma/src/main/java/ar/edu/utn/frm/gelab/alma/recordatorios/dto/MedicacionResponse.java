package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;

public record MedicacionResponse(
        Long id,
        TipoRecordatorio tipo,
        LocalDateTime fechaCreacion,
        String nombreMedicamento,
        String dosis,
        LocalTime horaToma,
        FrecuenciaToma frecuencia,
        LocalDate fechaInicio,
        int duracionDias) implements RecordatorioResponse {

    public static MedicacionResponse de(RecordatorioMedicacion medicacion) {
        return new MedicacionResponse(medicacion.getId(), TipoRecordatorio.MEDICACION,
                medicacion.getFechaCreacion(), medicacion.getNombreMedicamento(), medicacion.getDosis(),
                medicacion.getHoraToma(), medicacion.getFrecuencia(), medicacion.getFechaInicio(),
                medicacion.getDuracionDias());
    }
}
