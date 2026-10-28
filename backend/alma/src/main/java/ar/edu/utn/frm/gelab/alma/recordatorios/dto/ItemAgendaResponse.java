package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import java.time.LocalDateTime;

import org.hibernate.Hibernate;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioCita;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;

/**
 * Una notificación de la agenda con los datos de su recordatorio (contrato, 6.1).
 */
public record ItemAgendaResponse(
        Long notificacionId,
        LocalDateTime fechaHoraProgramada,
        EstadoNotificacion estado,
        LocalDateTime fechaHoraToma,
        boolean atrasada,
        RecordatorioResponse recordatorio) {

    /** "Atrasada" no se guarda: es una medicación PENDIENTE cuya hora ya pasó (contrato, 5.6). */
    public static ItemAgendaResponse de(NotificacionRecordatorio notificacion, LocalDateTime ahora) {
        // unproxy: si el recordatorio llegara como proxy de la clase base, el switch no lo reconocería.
        RecordatorioResponse recordatorio = switch (Hibernate.unproxy(notificacion.getRecordatorio())) {
            case RecordatorioMedicacion medicacion -> MedicacionResponse.de(medicacion);
            case RecordatorioCita cita -> CitaResponse.de(cita, notificacion);
            default -> throw new IllegalStateException("Tipo de recordatorio desconocido");
        };
        boolean atrasada = recordatorio instanceof MedicacionResponse
                && notificacion.getEstado() == EstadoNotificacion.PENDIENTE
                && notificacion.getFechaHoraProgramada().isBefore(ahora);
        return new ItemAgendaResponse(notificacion.getId(), notificacion.getFechaHoraProgramada(),
                notificacion.getEstado(), notificacion.getFechaHoraToma(), atrasada, recordatorio);
    }
}
