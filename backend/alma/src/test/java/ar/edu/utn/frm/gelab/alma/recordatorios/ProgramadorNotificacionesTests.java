package ar.edu.utn.frm.gelab.alma.recordatorios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioCita;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.NotificacionRecordatorioRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.service.ProgramadorNotificaciones;

/**
 * Calendario de tomas y regeneración de notificaciones (contrato, 5.1 a 5.3). Sin Spring ni base de datos.
 */
class ProgramadorNotificacionesTests {

    private static final Long CUENTA = 1L;
    private static final LocalDate INICIO = LocalDate.of(2026, 10, 28);
    private static final LocalTime HORA = LocalTime.of(9, 0);

    private NotificacionRecordatorioRepository repositorio;
    private ProgramadorNotificaciones programador;

    @BeforeEach
    void preparar() {
        repositorio = mock(NotificacionRecordatorioRepository.class);
        programador = new ProgramadorNotificaciones(repositorio);
    }

    // --- 5.1: los tres ejemplos del contrato ---

    @Test
    void diaria31DiasGenera31TomasDelHastaElSiguienteMes() {
        List<LocalDateTime> tomas = ProgramadorNotificaciones.calendario(
                INICIO, HORA, FrecuenciaToma.DIARIA, 31);

        assertThat(tomas).hasSize(31);
        assertThat(tomas.getFirst()).isEqualTo(dia(2026, 10, 28));
        assertThat(tomas.getLast()).isEqualTo(dia(2026, 11, 27));
    }

    @Test
    void diaPorMedio31DiasGenera16Tomas() {
        List<LocalDateTime> tomas = ProgramadorNotificaciones.calendario(
                INICIO, HORA, FrecuenciaToma.DIA_POR_MEDIO, 31);

        assertThat(tomas).hasSize(16);
        assertThat(tomas.get(1)).isEqualTo(dia(2026, 10, 30));
        assertThat(tomas.getLast()).isEqualTo(dia(2026, 11, 27));
    }

    @Test
    void semanal31DiasGenera5Tomas() {
        List<LocalDateTime> tomas = ProgramadorNotificaciones.calendario(
                INICIO, HORA, FrecuenciaToma.SEMANAL, 31);

        assertThat(tomas).containsExactly(dia(2026, 10, 28), dia(2026, 11, 4), dia(2026, 11, 11),
                dia(2026, 11, 18), dia(2026, 11, 25));
    }

    @Test
    void bordesDeDuracion() {
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.DIARIA, 1)).hasSize(1);
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.SEMANAL, 1)).hasSize(1);
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.SEMANAL, 7)).hasSize(1);
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.SEMANAL, 8)).hasSize(2);
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.DIARIA, 365)).hasSize(365);
        assertThat(ProgramadorNotificaciones.calendario(INICIO, HORA, FrecuenciaToma.DIA_POR_MEDIO, 365))
                .hasSize(183);
    }

    // --- 5.2: alta ---

    @Test
    void altaNoGeneraLasTomasQueYaPasaron() {
        RecordatorioMedicacion medicacion = medicacion(FrecuenciaToma.DIARIA, 3);
        LocalDateTime ahora = dia(2026, 10, 28).withHour(10); // la de las 09:00 de hoy ya pasó

        programador.programarMedicacion(medicacion, ahora);

        assertThat(guardadas()).extracting(NotificacionRecordatorio::getFechaHoraProgramada)
                .containsExactly(dia(2026, 10, 29), dia(2026, 10, 30));
        assertThat(guardadas()).allMatch(n -> n.getEstado() == EstadoNotificacion.PENDIENTE);
    }

    @Test
    void altaDeCitaGeneraUnaNotificacionPendiente() {
        RecordatorioCita cita = new RecordatorioCita();
        cita.setFechaHora(dia(2026, 10, 30).withHour(10).withMinute(30));

        programador.programarCita(cita);

        ArgumentCaptor<NotificacionRecordatorio> captor = ArgumentCaptor.forClass(NotificacionRecordatorio.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getFechaHoraProgramada()).isEqualTo(cita.getFechaHora());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
    }

    // --- 5.3: edición de medicación ---

    @Test
    void edicionConservaLasQueCoincidenBorraLasSobrantesYCreaLasQueFaltan() {
        // Calendario nuevo: DIARIA por 5 días desde el 28/10 → 28, 29, 30, 31/10 y 1/11 a las 09:00.
        RecordatorioMedicacion medicacion = medicacion(FrecuenciaToma.DIARIA, 5);
        LocalDateTime ahora = dia(2026, 10, 28).withHour(10); // el 28/10 ya pasó

        NotificacionRecordatorio pasada = notificacion(1L, dia(2026, 10, 27), EstadoNotificacion.TOMADA);
        NotificacionRecordatorio coincide = notificacion(2L, dia(2026, 10, 29), EstadoNotificacion.PENDIENTE);
        NotificacionRecordatorio sobrante = notificacion(3L, dia(2026, 11, 10), EstadoNotificacion.PENDIENTE);
        NotificacionRecordatorio tomadaAdelantada = notificacion(4L, dia(2026, 10, 30), EstadoNotificacion.TOMADA);
        NotificacionRecordatorio tomadaSobrante = notificacion(5L, dia(2026, 11, 11), EstadoNotificacion.TOMADA);
        when(repositorio.findByRecordatorioIdAndRecordatorioPersonaMayorCuentaId(medicacion.getId(), CUENTA))
                .thenReturn(List.of(pasada, coincide, sobrante, tomadaAdelantada, tomadaSobrante));

        programador.reprogramarMedicacion(medicacion, CUENTA, ahora);

        // Solo se borra la PENDIENTE futura que ya no está en el calendario: ni la pasada, ni las TOMADA.
        verify(repositorio).borrarPorIds(List.of(3L));
        // Se crean las que faltan: ni la que coincide (29/10) ni la TOMADA adelantada (30/10).
        assertThat(guardadas()).extracting(NotificacionRecordatorio::getFechaHoraProgramada)
                .containsExactly(dia(2026, 10, 31), dia(2026, 11, 1));
    }

    @Test
    void edicionSinCambiosNoBorraNiCreaNada() {
        RecordatorioMedicacion medicacion = medicacion(FrecuenciaToma.DIARIA, 3);
        LocalDateTime ahora = dia(2026, 10, 28).withHour(10);
        when(repositorio.findByRecordatorioIdAndRecordatorioPersonaMayorCuentaId(medicacion.getId(), CUENTA))
                .thenReturn(List.of(
                        notificacion(1L, dia(2026, 10, 29), EstadoNotificacion.PENDIENTE),
                        notificacion(2L, dia(2026, 10, 30), EstadoNotificacion.PENDIENTE)));

        programador.reprogramarMedicacion(medicacion, CUENTA, ahora);

        verify(repositorio, never()).borrarPorIds(any());
        assertThat(guardadas()).isEmpty();
    }

    // --- 5.3: edición de cita ---

    @Test
    void citaConLaMismaFechaNoTocaSuNotificacion() {
        RecordatorioCita cita = new RecordatorioCita();
        cita.setId(7L);

        programador.reprogramarCita(cita, false);

        verify(repositorio, never()).borrarDeRecordatorio(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void citaConOtraFechaBorraLaNotificacionYCreaUnaPendiente() {
        RecordatorioCita cita = new RecordatorioCita();
        cita.setId(7L);
        cita.setFechaHora(dia(2026, 11, 2));

        programador.reprogramarCita(cita, true);

        verify(repositorio).borrarDeRecordatorio(7L);
        ArgumentCaptor<NotificacionRecordatorio> captor = ArgumentCaptor.forClass(NotificacionRecordatorio.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
        assertThat(captor.getValue().getFechaHoraProgramada()).isEqualTo(dia(2026, 11, 2));
    }

    // --- ayudas ---

    private List<NotificacionRecordatorio> guardadas() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NotificacionRecordatorio>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repositorio).saveAll(captor.capture());
        return java.util.stream.StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }

    private static RecordatorioMedicacion medicacion(FrecuenciaToma frecuencia, int duracionDias) {
        RecordatorioMedicacion medicacion = new RecordatorioMedicacion();
        medicacion.setId(10L);
        medicacion.setFechaInicio(INICIO);
        medicacion.setHoraToma(HORA);
        medicacion.setFrecuencia(frecuencia);
        medicacion.setDuracionDias(duracionDias);
        return medicacion;
    }

    private static NotificacionRecordatorio notificacion(Long id, LocalDateTime momento, EstadoNotificacion estado) {
        NotificacionRecordatorio notificacion = new NotificacionRecordatorio(null, momento);
        notificacion.setId(id);
        notificacion.setEstado(estado);
        return notificacion;
    }

    /** Ese día a las 09:00. */
    private static LocalDateTime dia(int anio, int mes, int dia) {
        return LocalDate.of(anio, mes, dia).atTime(HORA);
    }
}
