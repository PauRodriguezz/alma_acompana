package ar.edu.utn.frm.gelab.alma.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.RecordatorioRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.service.RecordatorioService;

/**
 * Recordatorios de prueba (contrato de recordatorios, sección 11). Solo con alma.seed.enabled=true.
 * Escucha ApplicationReadyEvent, que Spring publica cuando ya terminaron todos los runners: así corre
 * siempre después de DatosDePrueba (que crea a ana y a carlos) sin depender del orden de los runners.
 * Es idempotente: solo carga si la persona todavía no tiene recordatorios.
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDePruebaRecordatorios {

    private static final Logger log = LoggerFactory.getLogger(DatosDePruebaRecordatorios.class);

    private static final int DURACION_DIAS = 31;

    private final CuentaRepository cuentaRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final RecordatorioService recordatorioService;
    private final Clock clock;

    public DatosDePruebaRecordatorios(CuentaRepository cuentaRepository,
                                      RecordatorioRepository recordatorioRepository,
                                      RecordatorioService recordatorioService,
                                      Clock clock) {
        this.cuentaRepository = cuentaRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.recordatorioService = recordatorioService;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        cargarDeAna();
        cargarDeCarlos();
    }

    private void cargarDeAna() {
        cuentaDeSinRecordatorios("ana").ifPresent(cuentaId -> {
            medicacion(cuentaId, "Pastilla de la mañana", LocalTime.of(9, 0));
            medicacion(cuentaId, "Pastilla del almuerzo", LocalTime.of(13, 0));
            medicacion(cuentaId, "Pastilla de la noche", LocalTime.of(21, 0));
            recordatorioService.crearCita(cuentaId, new CitaRequest("Control anual", "Cardiología",
                    "Dra. Pérez", "Consultorio Centro",
                    LocalDate.now(clock).plusDays(3).atTime(10, 30), null));
            log.info("Recordatorios de prueba creados para ana");
        });
    }

    private void cargarDeCarlos() {
        cuentaDeSinRecordatorios("carlos").ifPresent(cuentaId -> {
            medicacion(cuentaId, "Pastilla de la mañana", LocalTime.of(8, 0));
            log.info("Recordatorios de prueba creados para carlos");
        });
    }

    private void medicacion(Long cuentaId, String nombre, LocalTime hora) {
        recordatorioService.crearMedicacion(cuentaId, new MedicacionRequest(nombre, "1 pastilla", hora,
                FrecuenciaToma.DIARIA, null, DURACION_DIAS));
    }

    /** Id de la cuenta si existe y todavía no tiene recordatorios. */
    private Optional<Long> cuentaDeSinRecordatorios(String nombreUsuario) {
        return cuentaRepository.findByNombreUsuario(nombreUsuario)
                .map(cuenta -> cuenta.getId())
                .filter(cuentaId -> !recordatorioRepository.existsByPersonaMayorCuentaId(cuentaId));
    }
}
