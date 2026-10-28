package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioCita;

/**
 * Una cita tiene una sola notificación, por eso la respuesta incluye su id y su estado.
 */
public record CitaResponse(
        Long id,
        TipoRecordatorio tipo,
        LocalDateTime fechaCreacion,
        String titulo,
        String especialidad,
        String medico,
        String lugar,
        LocalDateTime fechaHora,
        String indicacionesPrevias,
        Long notificacionId,
        EstadoNotificacion estado) implements RecordatorioResponse {

    public static CitaResponse de(RecordatorioCita cita, NotificacionRecordatorio notificacion) {
        return new CitaResponse(cita.getId(), TipoRecordatorio.CITA, cita.getFechaCreacion(), cita.getTitulo(),
                cita.getEspecialidad(), cita.getMedico(), cita.getLugar(), cita.getFechaHora(),
                cita.getIndicacionesPrevias(), notificacion.getId(), notificacion.getEstado());
    }
}
