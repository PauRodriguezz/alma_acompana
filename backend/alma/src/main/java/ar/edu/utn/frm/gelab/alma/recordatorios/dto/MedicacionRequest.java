package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.utn.frm.gelab.alma.recordatorios.MensajesRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de una medicación (contrato, 5.8). Los textos se guardan sin espacios a los costados.
 * La regla de fechaInicio (no anterior a hoy) la controla el servicio, porque en la edición depende
 * de la fecha que ya tenía.
 */
public record MedicacionRequest(
        @NotBlank(message = MensajesRecordatorio.NOMBRE_OBLIGATORIO)
        @Size(max = MensajesRecordatorio.MAX_NOMBRE, message = MensajesRecordatorio.NOMBRE_LARGO)
        String nombreMedicamento,

        @NotBlank(message = MensajesRecordatorio.DOSIS_OBLIGATORIA)
        @Size(max = MensajesRecordatorio.MAX_DOSIS, message = MensajesRecordatorio.DOSIS_LARGA)
        String dosis,

        @NotNull(message = MensajesRecordatorio.HORA_OBLIGATORIA) LocalTime horaToma,

        @NotNull(message = MensajesRecordatorio.FRECUENCIA_OBLIGATORIA) FrecuenciaToma frecuencia,

        /** Opcional: en el alta, por defecto hoy; en la edición, por defecto la que ya tenía. */
        LocalDate fechaInicio,

        @NotNull(message = MensajesRecordatorio.DURACION_OBLIGATORIA)
        @Min(value = MensajesRecordatorio.MIN_DURACION_DIAS, message = MensajesRecordatorio.DURACION_FUERA_DE_RANGO)
        @Max(value = MensajesRecordatorio.MAX_DURACION_DIAS, message = MensajesRecordatorio.DURACION_FUERA_DE_RANGO)
        Integer duracionDias) {

    public MedicacionRequest {
        nombreMedicamento = Textos.recortar(nombreMedicamento);
        dosis = Textos.recortar(dosis);
    }
}
