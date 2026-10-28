package ar.edu.utn.frm.gelab.alma.recordatorios.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioCita;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.NotificacionRecordatorioRepository;

/**
 * Calcula el calendario de tomas (contrato, 5.1) y genera o regenera las notificaciones (5.2 y 5.3).
 */
@Component
public class ProgramadorNotificaciones {

    private final NotificacionRecordatorioRepository notificacionRepository;

    public ProgramadorNotificaciones(NotificacionRecordatorioRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * Hay una toma en fechaInicio + k × salto, a la hora indicada, mientras k × salto < duracionDias.
     */
    public static List<LocalDateTime> calendario(LocalDate fechaInicio, LocalTime horaToma,
            FrecuenciaToma frecuencia, int duracionDias) {
        List<LocalDateTime> tomas = new ArrayList<>();
        int salto = frecuencia.getSaltoDias();
        for (int dias = 0; dias < duracionDias; dias += salto) {
            tomas.add(fechaInicio.plusDays(dias).atTime(horaToma));
        }
        return tomas;
    }

    /** Alta de una medicación: las tomas del calendario que todavía no pasaron, como PENDIENTE. */
    public void programarMedicacion(RecordatorioMedicacion medicacion, LocalDateTime ahora) {
        List<NotificacionRecordatorio> nuevas = calendarioFuturo(medicacion, ahora).stream()
                .map(momento -> new NotificacionRecordatorio(medicacion, momento))
                .toList();
        notificacionRepository.saveAll(nuevas);
    }

    /** Alta de una cita: una sola notificación PENDIENTE en su fecha y hora. */
    public void programarCita(RecordatorioCita cita) {
        notificacionRepository.save(new NotificacionRecordatorio(cita, cita.getFechaHora()));
    }

    /**
     * Edición de una medicación. Se conservan (con su id y su estado) las notificaciones que coinciden
     * en el mismo instante con el calendario nuevo, se borran solo las PENDIENTE futuras que ya no
     * están en él y se crean las que faltan. Las pasadas y las TOMADA no se tocan.
     * El resultado es el mismo que borrar y recrear las PENDIENTE futuras.
     */
    public void reprogramarMedicacion(RecordatorioMedicacion medicacion, Long cuentaId, LocalDateTime ahora) {
        List<LocalDateTime> calendarioNuevo = calendarioFuturo(medicacion, ahora);
        Set<LocalDateTime> instantesNuevos = new HashSet<>(calendarioNuevo);

        List<NotificacionRecordatorio> existentes = notificacionRepository
                .findByRecordatorioIdAndRecordatorioPersonaMayorCuentaId(medicacion.getId(), cuentaId);

        List<Long> aBorrar = existentes.stream()
                .filter(n -> n.getEstado() == EstadoNotificacion.PENDIENTE)
                .filter(n -> n.getFechaHoraProgramada().isAfter(ahora))
                .filter(n -> !instantesNuevos.contains(n.getFechaHoraProgramada()))
                .map(NotificacionRecordatorio::getId)
                .toList();

        // Cualquier notificación existente en un instante del calendario nuevo cuenta como ya creada
        // (también una TOMADA adelantada), y evita chocar con la clave única.
        Set<LocalDateTime> instantesExistentes = new HashSet<>();
        existentes.forEach(n -> instantesExistentes.add(n.getFechaHoraProgramada()));

        List<NotificacionRecordatorio> aCrear = calendarioNuevo.stream()
                .filter(momento -> !instantesExistentes.contains(momento))
                .map(momento -> new NotificacionRecordatorio(medicacion, momento))
                .toList();

        if (!aBorrar.isEmpty()) {
            notificacionRepository.borrarPorIds(aBorrar);
        }
        notificacionRepository.saveAll(aCrear);
    }

    /**
     * Edición de una cita. Si la fecha cambió se borra la notificación y se crea una nueva PENDIENTE;
     * si no cambió, queda como estaba (con su estado).
     */
    public void reprogramarCita(RecordatorioCita cita, boolean cambioFechaHora) {
        if (!cambioFechaHora) {
            return;
        }
        notificacionRepository.borrarDeRecordatorio(cita.getId());
        programarCita(cita);
    }

    private static List<LocalDateTime> calendarioFuturo(RecordatorioMedicacion medicacion, LocalDateTime ahora) {
        return calendario(medicacion.getFechaInicio(), medicacion.getHoraToma(),
                medicacion.getFrecuencia(), medicacion.getDuracionDias()).stream()
                .filter(momento -> momento.isAfter(ahora))
                .toList();
    }
}
