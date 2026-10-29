package ar.edu.utn.frm.gelab.alma.recordatorios.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.MensajesRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.Recordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioCita;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.EstadoNotificacionRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.ItemAgendaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.RecordatorioResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.TipoRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.NotificacionRecordatorioRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.RecordatorioRepository;

/**
 * Altas, ediciones, bajas y consultas de recordatorios de la persona del token (CU-07, CU-08 y CU-09).
 * Todo dato se busca con la cuenta del token: lo que no es de la persona responde 404.
 * Es el punto de entrada para otros módulos (por ejemplo, Chat): se llama a este servicio, no al HTTP.
 */
@Service
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final NotificacionRecordatorioRepository notificacionRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final ProgramadorNotificaciones programador;
    private final Clock clock;

    public RecordatorioService(RecordatorioRepository recordatorioRepository,
                               NotificacionRecordatorioRepository notificacionRepository,
                               PersonaMayorRepository personaMayorRepository,
                               ProgramadorNotificaciones programador,
                               Clock clock) {
        this.recordatorioRepository = recordatorioRepository;
        this.notificacionRepository = notificacionRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.programador = programador;
        this.clock = clock;
    }

    /**
     * Notificaciones de la persona entre dos fechas (inclusive), con los datos de su recordatorio.
     * Sin tipo trae medicaciones y citas. Lo usa el controlador y también el módulo de Chat.
     */
    @Transactional(readOnly = true)
    public List<ItemAgendaResponse> agenda(Long cuentaId, LocalDate desde, LocalDate hasta,
            TipoRecordatorio tipo) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", MensajesRecordatorio.RANGO_INVERTIDO);
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MensajesRecordatorio.MAX_RANGO_AGENDA_DIAS) {
            throw new DatosInvalidosException("hasta", MensajesRecordatorio.RANGO_DEMASIADO_LARGO);
        }
        LocalDateTime ahora = LocalDateTime.now(clock);
        return notificacionRepository
                .agenda(cuentaId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), clasesDe(tipo)).stream()
                .map(notificacion -> ItemAgendaResponse.de(notificacion, ahora))
                .toList();
    }

    /**
     * Todos los recordatorios de la persona (también los que empiezan más adelante y los que ya
     * terminaron), por id ascendente. Sin tipo trae medicaciones y citas.
     */
    @Transactional(readOnly = true)
    public List<RecordatorioResponse> listar(Long cuentaId, TipoRecordatorio tipo) {
        List<Recordatorio> recordatorios = recordatorioRepository.listar(cuentaId, clasesDe(tipo));

        // Una cita lleva su única notificación: se piden todas juntas, no una consulta por cita.
        List<Long> idsDeCitas = recordatorios.stream()
                .filter(recordatorio -> recordatorio instanceof RecordatorioCita)
                .map(Recordatorio::getId)
                .toList();
        Map<Long, NotificacionRecordatorio> notificacionPorCita = idsDeCitas.isEmpty()
                ? Map.of()
                : notificacionRepository
                        .findByRecordatorioIdInAndRecordatorioPersonaMayorCuentaId(idsDeCitas, cuentaId).stream()
                        .collect(Collectors.toMap(notificacion -> notificacion.getRecordatorio().getId(),
                                Function.identity(), (primera, otra) -> primera));

        return recordatorios.stream()
                .<RecordatorioResponse>map(recordatorio -> switch (recordatorio) {
                    case RecordatorioMedicacion medicacion -> MedicacionResponse.de(medicacion);
                    case RecordatorioCita cita -> CitaResponse.de(cita, notificacionPorCita.get(cita.getId()));
                    default -> throw new IllegalStateException("Tipo de recordatorio desconocido");
                })
                .toList();
    }

    private static List<Class<? extends Recordatorio>> clasesDe(TipoRecordatorio tipo) {
        return switch (tipo) {
            case MEDICACION -> List.of(RecordatorioMedicacion.class);
            case CITA -> List.of(RecordatorioCita.class);
            case null -> List.of(RecordatorioMedicacion.class, RecordatorioCita.class);
        };
    }

    @Transactional(readOnly = true)
    public RecordatorioResponse detalle(Long cuentaId, Long id) {
        return switch (recordatorioDe(cuentaId, id)) {
            case RecordatorioMedicacion medicacion -> MedicacionResponse.de(medicacion);
            case RecordatorioCita cita -> CitaResponse.de(cita, notificacionDeCita(cuentaId, cita));
            default -> throw new RecursoNoEncontradoException();
        };
    }

    @Transactional
    public MedicacionResponse crearMedicacion(Long cuentaId, MedicacionRequest datos) {
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuentaId)
                .orElseThrow(RecursoNoEncontradoException::new);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        LocalDate fechaInicio = datos.fechaInicio() != null ? datos.fechaInicio() : hoy;
        if (fechaInicio.isBefore(hoy)) {
            throw new DatosInvalidosException("fechaInicio", MensajesRecordatorio.FECHA_INICIO_PASADA);
        }

        RecordatorioMedicacion medicacion = new RecordatorioMedicacion();
        medicacion.setPersonaMayor(persona);
        medicacion.setFechaCreacion(ahora);
        aplicar(medicacion, datos, fechaInicio);
        recordatorioRepository.save(medicacion);
        programador.programarMedicacion(medicacion, ahora);
        return MedicacionResponse.de(medicacion);
    }

    @Transactional
    public CitaResponse crearCita(Long cuentaId, CitaRequest datos) {
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuentaId)
                .orElseThrow(RecursoNoEncontradoException::new);
        LocalDateTime ahora = LocalDateTime.now(clock);
        validarFechaHoraFutura(datos.fechaHora(), ahora);

        RecordatorioCita cita = new RecordatorioCita();
        cita.setPersonaMayor(persona);
        cita.setFechaCreacion(ahora);
        aplicar(cita, datos);
        recordatorioRepository.save(cita);
        programador.programarCita(cita);
        return CitaResponse.de(cita, notificacionDeCita(cuentaId, cita));
    }

    /** Editar como medicación algo que no es una medicación responde 404. */
    @Transactional
    public MedicacionResponse editarMedicacion(Long cuentaId, Long id, MedicacionRequest datos) {
        if (!(recordatorioDe(cuentaId, id) instanceof RecordatorioMedicacion medicacion)) {
            throw new RecursoNoEncontradoException();
        }
        LocalDateTime ahora = LocalDateTime.now(clock);

        // Sin fechaInicio se conserva la que tenía. Si la cambia, no puede ser anterior a hoy.
        LocalDate fechaInicio = datos.fechaInicio() != null ? datos.fechaInicio() : medicacion.getFechaInicio();
        if (!fechaInicio.equals(medicacion.getFechaInicio()) && fechaInicio.isBefore(ahora.toLocalDate())) {
            throw new DatosInvalidosException("fechaInicio", MensajesRecordatorio.FECHA_INICIO_PASADA);
        }

        aplicar(medicacion, datos, fechaInicio);
        // Reprogramar puede limpiar el contexto de persistencia: los cambios van antes.
        programador.reprogramarMedicacion(medicacion, cuentaId, ahora);
        return MedicacionResponse.de(medicacion);
    }

    /** Editar como cita algo que no es una cita responde 404. */
    @Transactional
    public CitaResponse editarCita(Long cuentaId, Long id, CitaRequest datos) {
        if (!(recordatorioDe(cuentaId, id) instanceof RecordatorioCita cita)) {
            throw new RecursoNoEncontradoException();
        }
        validarFechaHoraFutura(datos.fechaHora(), LocalDateTime.now(clock));

        boolean cambioFechaHora = !datos.fechaHora().equals(cita.getFechaHora());
        aplicar(cita, datos);
        programador.reprogramarCita(cita, cambioFechaHora);
        return CitaResponse.de(cita, notificacionDeCita(cuentaId, cita));
    }

    /** Borra el recordatorio y todas sus notificaciones, pasadas o futuras. */
    @Transactional
    public void eliminar(Long cuentaId, Long id) {
        Recordatorio recordatorio = recordatorioDe(cuentaId, id);
        notificacionRepository.borrarDeRecordatorio(recordatorio.getId());
        recordatorioRepository.delete(recordatorio);
    }

    /**
     * Medicación: PENDIENTE o TOMADA. Cita: PENDIENTE o VISTA. Pedir el estado actual no cambia nada.
     */
    @Transactional
    public ItemAgendaResponse cambiarEstado(Long cuentaId, Long notificacionId, EstadoNotificacionRequest datos) {
        NotificacionRecordatorio notificacion = notificacionRepository
                .findByIdAndRecordatorioPersonaMayorCuentaId(notificacionId, cuentaId)
                .orElseThrow(RecursoNoEncontradoException::new);
        boolean esMedicacion = Hibernate.unproxy(notificacion.getRecordatorio()) instanceof RecordatorioMedicacion;
        EstadoNotificacion estado = datos.estado();
        EstadoNotificacion propio = esMedicacion ? EstadoNotificacion.TOMADA : EstadoNotificacion.VISTA;
        if (estado != EstadoNotificacion.PENDIENTE && estado != propio) {
            throw new DatosInvalidosException("estado", MensajesRecordatorio.ESTADO_NO_CORRESPONDE);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        switch (estado) {
            case TOMADA -> notificacion.marcarTomada(ahora);
            case VISTA -> notificacion.marcarVista();
            case PENDIENTE -> {
                if (notificacion.getEstado() != EstadoNotificacion.PENDIENTE) {
                    notificacion.marcarPendiente();
                }
            }
        }
        return ItemAgendaResponse.de(notificacion, ahora);
    }

    private Recordatorio recordatorioDe(Long cuentaId, Long id) {
        return recordatorioRepository.findByIdAndPersonaMayorCuentaId(id, cuentaId)
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private NotificacionRecordatorio notificacionDeCita(Long cuentaId, RecordatorioCita cita) {
        return notificacionRepository
                .findByRecordatorioIdAndRecordatorioPersonaMayorCuentaId(cita.getId(), cuentaId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La cita " + cita.getId() + " no tiene notificación"));
    }

    private static void validarFechaHoraFutura(LocalDateTime fechaHora, LocalDateTime ahora) {
        if (!fechaHora.isAfter(ahora)) {
            throw new DatosInvalidosException("fechaHora", MensajesRecordatorio.FECHA_HORA_PASADA);
        }
    }

    private static void aplicar(RecordatorioMedicacion medicacion, MedicacionRequest datos, LocalDate fechaInicio) {
        medicacion.setNombreMedicamento(datos.nombreMedicamento());
        medicacion.setDosis(datos.dosis());
        medicacion.setHoraToma(datos.horaToma());
        medicacion.setFrecuencia(datos.frecuencia());
        medicacion.setFechaInicio(fechaInicio);
        medicacion.setDuracionDias(datos.duracionDias());
    }

    private static void aplicar(RecordatorioCita cita, CitaRequest datos) {
        cita.setTitulo(datos.titulo());
        cita.setEspecialidad(datos.especialidad());
        cita.setMedico(datos.medico());
        cita.setLugar(datos.lugar());
        cita.setFechaHora(datos.fechaHora());
        cita.setIndicacionesPrevias(datos.indicacionesPrevias());
    }
}
