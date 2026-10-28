package ar.edu.utn.frm.gelab.alma.recordatorios.dto;

import ar.edu.utn.frm.gelab.alma.recordatorios.MensajesRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import jakarta.validation.constraints.NotNull;

/**
 * Nuevo estado de una notificación (contrato, 6.7).
 */
public record EstadoNotificacionRequest(
        @NotNull(message = MensajesRecordatorio.ESTADO_OBLIGATORIO) EstadoNotificacion estado) {
}
