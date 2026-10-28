package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.recordatorios.MensajesRecordatorio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta y edición de una cita (contrato, 5.8). Que fechaHora sea posterior a ahora lo controla el
 * servicio con el Clock, tanto al crear como al editar.
 */
public record CitaRequest(
        @NotBlank(message = MensajesRecordatorio.TITULO_OBLIGATORIO)
        @Size(max = MensajesRecordatorio.MAX_TITULO, message = MensajesRecordatorio.TITULO_LARGO)
        String titulo,

        @NotBlank(message = MensajesRecordatorio.ESPECIALIDAD_OBLIGATORIA)
        @Size(max = MensajesRecordatorio.MAX_ESPECIALIDAD, message = MensajesRecordatorio.ESPECIALIDAD_LARGA)
        String especialidad,

        @NotBlank(message = MensajesRecordatorio.MEDICO_OBLIGATORIO)
        @Size(max = MensajesRecordatorio.MAX_MEDICO, message = MensajesRecordatorio.MEDICO_LARGO)
        String medico,

        @NotBlank(message = MensajesRecordatorio.LUGAR_OBLIGATORIO)
        @Size(max = MensajesRecordatorio.MAX_LUGAR, message = MensajesRecordatorio.LUGAR_LARGO)
        String lugar,

        @NotNull(message = MensajesRecordatorio.FECHA_HORA_OBLIGATORIA) LocalDateTime fechaHora,

        @Size(max = MensajesRecordatorio.MAX_INDICACIONES, message = MensajesRecordatorio.INDICACIONES_LARGAS)
        String indicacionesPrevias) {

    public CitaRequest {
        titulo = Textos.recortar(titulo);
        especialidad = Textos.recortar(especialidad);
        medico = Textos.recortar(medico);
        lugar = Textos.recortar(lugar);
        indicacionesPrevias = Textos.recortarOpcional(indicacionesPrevias);
    }
}
