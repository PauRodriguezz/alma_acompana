package ar.edu.utn.frm.gelab.alma.recordatorios;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.FrecuenciaToma;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.RecordatorioMedicacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.NotificacionRecordatorioRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.repository.RecordatorioRepository;

/**
 * Pruebas del contrato de recordatorios (sección 13). Usan el Postgres de Docker ("docker compose up -d").
 * Crean sus propias dos cuentas de prueba (sin el seed) y las borran al terminar: no tocan a ana ni a
 * carlos. El reloj de la aplicación queda fijo en el 28/10/2026 a las 10:00.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(RecordatorioIntegrationTests.RelojFijo.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecordatorioIntegrationTests {

    private static final String BASE = "/api/recordatorios";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 28, 10, 0);
    private static final String USUARIO_A = "test_rec_a";
    private static final String USUARIO_B = "test_rec_b";

    /** Reloj fijo: "ahora" es el 28/10/2026 a las 10:00. */
    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RecordatorioRepository recordatorioRepository;
    @Autowired
    private NotificacionRecordatorioRepository notificacionRepository;

    private Long cuentaA;
    private Long cuentaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeAll
    void crearCuentas() {
        cuentaA = cuentaDePrueba(USUARIO_A, "Test", "A");
        cuentaB = cuentaDePrueba(USUARIO_B, "Test", "B");
        tokenA = token(cuentaA, "PERSONA_MAYOR");
        tokenB = token(cuentaB, "PERSONA_MAYOR");
        tokenAdmin = token(999_999L, "ADMINISTRADOR");
    }

    @BeforeEach
    @AfterEach
    void limpiarRecordatorios() {
        String propias = "select p.id from persona_mayor p where p.cuenta_id in (?, ?)";
        String recordatoriosPropios = "select r.id from recordatorio r where r.persona_mayor_id in (" + propias + ")";
        jdbc.update("delete from notificacion_recordatorio where recordatorio_id in (" + recordatoriosPropios + ")",
                cuentaA, cuentaB);
        jdbc.update("delete from recordatorio_medicacion where id in (" + recordatoriosPropios + ")", cuentaA, cuentaB);
        jdbc.update("delete from recordatorio_cita where id in (" + recordatoriosPropios + ")", cuentaA, cuentaB);
        jdbc.update("delete from recordatorio where persona_mayor_id in (" + propias + ")", cuentaA, cuentaB);
    }

    @AfterAll
    void borrarCuentas() {
        for (String usuario : List.of(USUARIO_A, USUARIO_B)) {
            cuentaRepository.findByNombreUsuario(usuario).ifPresent(cuenta -> {
                personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(personaMayorRepository::delete);
                cuentaRepository.delete(cuenta);
            });
        }
    }

    // --- Acceso ---

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(BASE + "/agenda?desde=2026-10-28&hasta=2026-10-28"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
    }

    @Test
    void administradorDevuelve403() throws Exception {
        mockMvc.perform(get(BASE + "/agenda?desde=2026-10-28&hasta=2026-10-28").header(HttpHeaders.AUTHORIZATION,
                bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        mockMvc.perform(post(BASE + "/citas").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON).content(cita("2026-11-02T10:30")))
                .andExpect(status().isForbidden());
    }

    // --- Alta de medicación (5.1 y 5.2) ---

    @Test
    void altaDiariaPor31DiasNoGeneraLasTomasDeHoyQueYaPasaron() throws Exception {
        // Hoy son las 10:00: la toma de las 09:00 de hoy ya pasó, quedan 30 (del 29/10 al 27/11).
        mockMvc.perform(post(BASE + "/medicacion").header(HttpHeaders.AUTHORIZATION, bearer(tokenA))
                .contentType(MediaType.APPLICATION_JSON).content(medicacion("DIARIA", 31, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("MEDICACION"))
                .andExpect(jsonPath("$.fechaCreacion").value("2026-10-28T10:00:00"))
                .andExpect(jsonPath("$.fechaInicio").value("2026-10-28"))
                .andExpect(jsonPath("$.horaToma").value("09:00:00"));

        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(30)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-10-29T09:00:00"))
                .andExpect(jsonPath("$[29].fechaHoraProgramada").value("2026-11-27T09:00:00"));
    }

    @Test
    void altaDiaPorMedioPor31DiasGenera16Notificaciones() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIA_POR_MEDIO", 31, "2026-10-29"));

        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-10-29T09:00:00"))
                .andExpect(jsonPath("$[15].fechaHoraProgramada").value("2026-11-28T09:00:00"));
    }

    @Test
    void altaSemanalPor31DiasGenera5Notificaciones() throws Exception {
        crear(tokenA, "/medicacion", medicacion("SEMANAL", 31, "2026-10-29"));

        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[4].fechaHoraProgramada").value("2026-11-26T09:00:00"));
    }

    @Test
    void medicacionConDatosInvalidosDevuelve400ConErrores() throws Exception {
        enviar(post(BASE + "/medicacion"), tokenA, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores", hasSize(5)))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("horaToma")))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("frecuencia")));

        for (int dias : new int[] { 0, 366 }) {
            enviar(post(BASE + "/medicacion"), tokenA, medicacion("DIARIA", dias, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("duracionDias"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value("Los días tienen que ser entre 1 y 365."));
        }
    }

    @Test
    void medicacionConFechaDeInicioPasadaDevuelve400() throws Exception {
        enviar(post(BASE + "/medicacion"), tokenA, medicacion("DIARIA", 5, "2026-10-27"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaInicio"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("La fecha de inicio no puede ser anterior a hoy."));
    }

    @Test
    void horaOFrecuenciaConFormatoInvalidoDevuelve400SinLista() throws Exception {
        enviar(post(BASE + "/medicacion"), tokenA, medicacion("DIARIA", 5, null).replace("09:00", "25:99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores").doesNotExist());
    }

    @Test
    void losTextosSeGuardanSinEspaciosYSoloEspaciosCuentaComoVacio() throws Exception {
        String conEspacios = medicacion("DIARIA", 5, "2026-10-29").replace("Enalapril", "   Enalapril  ");
        enviar(post(BASE + "/medicacion"), tokenA, conEspacios)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreMedicamento").value("Enalapril"));

        enviar(post(BASE + "/medicacion"), tokenA, medicacion("DIARIA", 5, null).replace("Enalapril", "    "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombreMedicamento"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Escribí el nombre del medicamento."));
    }

    // --- Alta de cita (5.2 y 5.8) ---

    @Test
    void citaFuturaDevuelve201ConUnaNotificacionPendiente() throws Exception {
        enviar(post(BASE + "/citas"), tokenA, cita("2026-10-30T10:30"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CITA"))
                .andExpect(jsonPath("$.notificacionId", notNullValue()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fechaHora").value("2026-10-30T10:30:00"));

        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-10-30T10:30:00"))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].atrasada").value(false));
    }

    @Test
    void citaConFechaPasadaOIgualAAhoraDevuelve400() throws Exception {
        for (String fechaHora : List.of("2026-10-28T09:00", "2026-10-28T10:00")) {
            enviar(post(BASE + "/citas"), tokenA, cita(fechaHora))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("fechaHora"))
                    .andExpect(jsonPath("$.errores[0].mensaje")
                            .value("La fecha y la hora tienen que ser posteriores a este momento."));
        }
    }

    @Test
    void citaSinDatosDevuelve400ConUnErrorPorCampo() throws Exception {
        enviar(post(BASE + "/citas"), tokenA, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasSize(5)));
    }

    // --- Detalle ---

    @Test
    void detalleDeCitaIncluyeNotificacionIdYEstadoYElDeMedicacionNo() throws Exception {
        long idCita = idDe(crear(tokenA, "/citas", cita("2026-10-30T10:30")));
        long idMedicacion = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29")));

        mockMvc.perform(get(BASE + "/" + idCita).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("CITA"))
                .andExpect(jsonPath("$.titulo").value("Control anual"))
                .andExpect(jsonPath("$.notificacionId", notNullValue()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mockMvc.perform(get(BASE + "/" + idMedicacion).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MEDICACION"))
                .andExpect(jsonPath("$.frecuencia").value("DIARIA"))
                .andExpect(jsonPath("$.notificacionId").doesNotExist())
                .andExpect(jsonPath("$.estado").doesNotExist());
    }

    // --- Otra persona: siempre 404 ---

    @Test
    void otraPersonaRecibe404AlPedirEditarBorrarOCambiarElEstadoYNoVeNadaEnLaAgenda() throws Exception {
        String cita = crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        long idCita = idDe(cita);
        long notificacionCita = ((Number) JsonPath.read(cita, "$.notificacionId")).longValue();
        long idMedicacion = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29")));

        mockMvc.perform(get(BASE + "/" + idCita).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos lo que buscás."));
        enviar(put(BASE + "/citas/" + idCita), tokenB, cita("2026-11-05T10:30")).andExpect(status().isNotFound());
        enviar(put(BASE + "/medicacion/" + idMedicacion), tokenB, medicacion("DIARIA", 3, null))
                .andExpect(status().isNotFound());
        enviar(put(BASE + "/notificaciones/" + notificacionCita + "/estado"), tokenB, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/" + idMedicacion).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isNotFound());

        agenda(tokenB, "2026-10-28", "2026-11-30", null).andExpect(jsonPath("$", hasSize(0)));
        // Nada cambió para la dueña.
        mockMvc.perform(get(BASE + "/" + idMedicacion).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk());
        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$[?(@.notificacionId==" + notificacionCita + ")].estado").value("PENDIENTE"));
    }

    // --- Edición de medicación (5.3) ---

    @Test
    void editarMedicacionConservaLasQueCoincidenBorraLasSobrantesYCreaLasQueFaltan() throws Exception {
        long id = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 5, "2026-10-29")));
        String original = agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(5))).andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(original, "$[*].notificacionId");
        enviar(put(BASE + "/notificaciones/" + ids.get(0) + "/estado"), tokenA, "{\"estado\":\"TOMADA\"}")
                .andExpect(status().isOk());

        // Mismo horario, 3 días: se conservan las 3 primeras (con su id y su estado) y se borran las otras 2.
        // Sin fechaInicio en el PUT se conserva la que ya tenía.
        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 3, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-10-29"));
        String tras = agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estado").value("TOMADA"))
                .andReturn().getResponse().getContentAsString();
        List<Number> idsTras = JsonPath.read(tras, "$[*].notificacionId");
        org.assertj.core.api.Assertions.assertThat(idsTras).containsExactlyElementsOf(ids.subList(0, 3));

        // Otra hora: la TOMADA futura no se toca; las PENDIENTE futuras se reemplazan por las de las 18:00.
        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 3, null).replace("09:00", "18:00"))
                .andExpect(status().isOk());
        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-10-29T09:00:00"))
                .andExpect(jsonPath("$[0].estado").value("TOMADA"))
                .andExpect(jsonPath("$[1].fechaHoraProgramada").value("2026-10-29T18:00:00"))
                .andExpect(jsonPath("$[3].fechaHoraProgramada").value("2026-10-31T18:00:00"));
    }

    @Test
    void editarMedicacionNoTocaLasNotificacionesQueYaPasaron() throws Exception {
        long id = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-28")));
        // La toma de hoy a las 09:00 ya pasó y no se generó: se agrega una pendiente vencida.
        guardarNotificacion(id, LocalDateTime.of(2026, 10, 28, 9, 0), EstadoNotificacion.PENDIENTE);

        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 2, "2026-10-28"))
                .andExpect(status().isOk());

        agenda(tokenA, "2026-10-28", "2026-10-31", null)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-10-28T09:00:00"))
                .andExpect(jsonPath("$[0].atrasada").value(true))
                .andExpect(jsonPath("$[1].fechaHoraProgramada").value("2026-10-29T09:00:00"));
    }

    @Test
    void editarMedicacionAceptaLaFechaDeInicioQueYaTeniaAunqueSeaPasada() throws Exception {
        long id = medicacionConInicioPasado(LocalDate.of(2026, 10, 20));

        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 31, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-10-20"));
        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 31, "2026-10-20"))
                .andExpect(status().isOk());
        // Cambiarla a otra fecha pasada no se acepta.
        enviar(put(BASE + "/medicacion/" + id), tokenA, medicacion("DIARIA", 31, "2026-10-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaInicio"));
    }

    // --- Edición de cita (5.3) ---

    @Test
    void editarCitaConLaMismaFechaConservaLaNotificacionYSuEstado() throws Exception {
        String creada = crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        long id = idDe(creada);
        long notificacionId = ((Number) JsonPath.read(creada, "$.notificacionId")).longValue();
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isOk());

        enviar(put(BASE + "/citas/" + id), tokenA, cita("2026-10-30T10:30").replace("Consultorio Centro", "Sanatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lugar").value("Sanatorio"))
                .andExpect(jsonPath("$.notificacionId").value(notificacionId))
                .andExpect(jsonPath("$.estado").value("VISTA"));
    }

    @Test
    void editarCitaConOtraFechaCreaUnaNotificacionNuevaPendiente() throws Exception {
        String creada = crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        long id = idDe(creada);
        long notificacionId = ((Number) JsonPath.read(creada, "$.notificacionId")).longValue();
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isOk());

        enviar(put(BASE + "/citas/" + id), tokenA, cita("2026-11-02T11:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.notificacionId", not(notificacionId)));

        agenda(tokenA, "2026-10-28", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fechaHoraProgramada").value("2026-11-02T11:00:00"));
    }

    @Test
    void editarCitaConFechaPasadaDevuelve400() throws Exception {
        long id = idDe(crear(tokenA, "/citas", cita("2026-10-30T10:30")));

        enviar(put(BASE + "/citas/" + id), tokenA, cita("2026-10-27T10:30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("fechaHora"));
    }

    @Test
    void editarPorLaRutaDelOtroTipoDevuelve404() throws Exception {
        long idCita = idDe(crear(tokenA, "/citas", cita("2026-10-30T10:30")));
        long idMedicacion = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29")));

        enviar(put(BASE + "/medicacion/" + idCita), tokenA, medicacion("DIARIA", 3, null))
                .andExpect(status().isNotFound());
        enviar(put(BASE + "/citas/" + idMedicacion), tokenA, cita("2026-10-30T10:30"))
                .andExpect(status().isNotFound());
    }

    // --- Baja (5.4) ---

    @Test
    void eliminarDevuelve204YBorraTambienSusNotificaciones() throws Exception {
        long idMedicacion = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 5, "2026-10-29")));
        long idCita = idDe(crear(tokenA, "/citas", cita("2026-10-30T10:30")));
        guardarNotificacion(idMedicacion, LocalDateTime.of(2026, 10, 27, 9, 0), EstadoNotificacion.PENDIENTE);

        mockMvc.perform(delete(BASE + "/" + idMedicacion).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + idMedicacion).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isNotFound());
        agenda(tokenA, "2026-10-20", "2026-11-30", null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recordatorio.id").value(idCita));
        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select count(*) from notificacion_recordatorio where recordatorio_id = ?",
                        Integer.class, idMedicacion)).isZero();

        mockMvc.perform(delete(BASE + "/" + idCita).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isNoContent());
        agenda(tokenA, "2026-10-20", "2026-11-30", null).andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Estado de una notificación (5.5) ---

    @Test
    void medicacionSeMarcaTomadaYSeDeshace() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29"));
        long notificacionId = primeraNotificacion(tokenA);

        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"TOMADA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificacionId").value(notificacionId))
                .andExpect(jsonPath("$.estado").value("TOMADA"))
                .andExpect(jsonPath("$.fechaHoraToma").value("2026-10-28T10:00:00"))
                .andExpect(jsonPath("$.recordatorio.tipo").value("MEDICACION"));
        // Repetir el mismo estado no cambia nada.
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"TOMADA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaHoraToma").value("2026-10-28T10:00:00"));
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"PENDIENTE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.fechaHoraToma", nullValue()));
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"PENDIENTE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void citaSeMarcaVistaYNuncaTieneFechaHoraToma() throws Exception {
        crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        long notificacionId = primeraNotificacion(tokenA);

        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VISTA"))
                .andExpect(jsonPath("$.fechaHoraToma", nullValue()))
                .andExpect(jsonPath("$.recordatorio.estado").value("VISTA"));
        enviar(put(BASE + "/notificaciones/" + notificacionId + "/estado"), tokenA, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VISTA"));
    }

    @Test
    void unEstadoQueNoCorrespondeAlTipoODesconocidoDevuelve400() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29"));
        long medicacion = primeraNotificacion(tokenA);
        crear(tokenA, "/citas", cita("2026-10-28T11:00"));
        long cita = notificacionDeTipo(tokenA, "CITA");

        enviar(put(BASE + "/notificaciones/" + medicacion + "/estado"), tokenA, "{\"estado\":\"VISTA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("estado"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Ese estado no corresponde a este recordatorio."));
        enviar(put(BASE + "/notificaciones/" + cita + "/estado"), tokenA, "{\"estado\":\"TOMADA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("estado"));
        enviar(put(BASE + "/notificaciones/" + cita + "/estado"), tokenA, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("estado"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Indicá el estado."));
        enviar(put(BASE + "/notificaciones/" + cita + "/estado"), tokenA, "{\"estado\":\"OTRO\"}")
                .andExpect(status().isBadRequest());
    }

    // --- Agenda (6.1 y 5.6) ---

    @Test
    void agendaFiltraPorRangoYPorTipo() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29"));
        crear(tokenA, "/citas", cita("2026-10-30T10:30"));

        agenda(tokenA, "2026-10-29", "2026-10-29", null).andExpect(jsonPath("$", hasSize(1)));
        agenda(tokenA, "2026-10-29", "2026-10-30", null)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].recordatorio.tipo").value("MEDICACION"))
                .andExpect(jsonPath("$[1].recordatorio.tipo").value("MEDICACION"))
                .andExpect(jsonPath("$[2].recordatorio.tipo").value("CITA"))
                .andExpect(jsonPath("$[2].recordatorio.notificacionId", notNullValue()));
        agenda(tokenA, "2026-10-29", "2026-10-31", "CITA")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recordatorio.tipo").value("CITA"));
        agenda(tokenA, "2026-10-29", "2026-10-31", "MEDICACION").andExpect(jsonPath("$", hasSize(3)));
        agenda(tokenA, "2026-11-10", "2026-11-12", null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void atrasadaEsTrueSoloEnMedicacionesPendientesQueYaPasaron() throws Exception {
        long id = idDe(crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29")));
        guardarNotificacion(id, LocalDateTime.of(2026, 10, 28, 6, 0), EstadoNotificacion.PENDIENTE);
        guardarNotificacion(id, LocalDateTime.of(2026, 10, 28, 7, 0), EstadoNotificacion.TOMADA);
        guardarNotificacion(id, LocalDateTime.of(2026, 10, 28, 23, 0), EstadoNotificacion.PENDIENTE);
        crear(tokenA, "/citas", cita("2026-10-28T11:00"));

        agenda(tokenA, "2026-10-28", "2026-10-28", null)
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].atrasada").value(true))
                .andExpect(jsonPath("$[1].estado").value("TOMADA"))
                .andExpect(jsonPath("$[1].atrasada").value(false))
                .andExpect(jsonPath("$[2].recordatorio.tipo").value("CITA"))
                .andExpect(jsonPath("$[2].atrasada").value(false))
                .andExpect(jsonPath("$[3].atrasada").value(false));
    }

    @Test
    void agendaRechazaRangosYParametrosInvalidos() throws Exception {
        // 62 días contando los dos extremos es el máximo.
        agenda(tokenA, "2026-10-28", "2026-12-28", null).andExpect(status().isOk());
        agenda(tokenA, "2026-10-28", "2026-12-29", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("hasta"));
        agenda(tokenA, "2026-10-28", "2026-10-27", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("hasta"));

        mockMvc.perform(get(BASE + "/agenda?desde=2026-10-28").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/agenda?desde=28/10/2026&hasta=2026-10-29")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isBadRequest());
        agenda(tokenA, "2026-10-28", "2026-10-29", "OTRO").andExpect(status().isBadRequest());
    }

    // --- Listado (6.8) ---

    @Test
    void listarSinTokenDevuelve401YAdministradorDevuelve403() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
    }

    @Test
    void listarDevuelveTodoLoDeLaPersonaTambienLoQueEmpiezaMasAdelanteYLoQueYaTermino() throws Exception {
        // La API no deja crear una medicación con inicio pasado: la que ya terminó se inserta directo.
        long terminada = medicacionConInicioPasado(LocalDate.of(2026, 8, 1));
        long futura = idDe(crear(tokenA, "/medicacion", medicacion("SEMANAL", 31, "2026-11-10")));
        String cita = crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        long idCita = idDe(cita);
        long notificacionCita = ((Number) JsonPath.read(cita, "$.notificacionId")).longValue();

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Por id ascendente
                .andExpect(jsonPath("$[0].id").value(terminada))
                .andExpect(jsonPath("$[0].tipo").value("MEDICACION"))
                .andExpect(jsonPath("$[0].fechaInicio").value("2026-08-01"))
                .andExpect(jsonPath("$[0].nombreMedicamento").value("Enalapril"))
                .andExpect(jsonPath("$[0].notificacionId").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(futura))
                .andExpect(jsonPath("$[1].fechaInicio").value("2026-11-10"))
                .andExpect(jsonPath("$[1].frecuencia").value("SEMANAL"))
                .andExpect(jsonPath("$[1].estado").doesNotExist())
                // La cita trae su notificación y su estado
                .andExpect(jsonPath("$[2].id").value(idCita))
                .andExpect(jsonPath("$[2].tipo").value("CITA"))
                .andExpect(jsonPath("$[2].titulo").value("Control anual"))
                .andExpect(jsonPath("$[2].notificacionId").value(notificacionCita))
                .andExpect(jsonPath("$[2].estado").value("PENDIENTE"));
    }

    @Test
    void listarFiltraPorTipoYUnTipoInvalidoDevuelve400() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29"));
        crear(tokenA, "/citas", cita("2026-10-30T10:30"));

        mockMvc.perform(get(BASE + "?tipo=MEDICACION").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("MEDICACION"));
        mockMvc.perform(get(BASE + "?tipo=CITA").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("CITA"))
                .andExpect(jsonPath("$[0].notificacionId", notNullValue()));
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(jsonPath("$", hasSize(2)));
        // Mismo manejo de error que el tipo inválido de la agenda
        mockMvc.perform(get(BASE + "?tipo=OTRO").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores").doesNotExist());
    }

    @Test
    void listarSoloDevuelveLoDeLaPersonaDelTokenYUnaPersonaSinNadaRecibeListaVacia() throws Exception {
        crear(tokenA, "/medicacion", medicacion("DIARIA", 3, "2026-10-29"));
        crear(tokenA, "/citas", cita("2026-10-30T10:30"));
        crear(tokenB, "/citas", cita("2026-11-03T09:00"));

        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fechaHora").value("2026-11-03T09:00:00"));
        mockMvc.perform(get(BASE + "?tipo=MEDICACION").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Sin nada cargado: lista vacía, no 404
        limpiarRecordatorios();
        mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Ayudas ---

    private Long cuentaDePrueba(String nombreUsuario, String nombre, String apellido) {
        return cuentaRepository.findByNombreUsuario(nombreUsuario).map(Cuenta::getId).orElseGet(() -> {
            Cuenta cuenta = new Cuenta();
            cuenta.setNombreUsuario(nombreUsuario);
            cuenta.setContrasenia(passwordEncoder.encode("Alma2026!"));
            cuenta.setRol(Rol.PERSONA_MAYOR);
            cuenta.setFechaAlta(LocalDate.now());
            cuenta = cuentaRepository.save(cuenta);

            PreferenciaAccesibilidad preferencia = new PreferenciaAccesibilidad();
            preferencia.setTamanioLetra(TamanioLetra.NORMAL);
            preferencia.setAltoContraste(false);
            PersonaMayor persona = new PersonaMayor();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setCuenta(cuenta);
            persona.setPreferenciaAccesibilidad(preferencia);
            personaMayorRepository.save(persona);
            return cuenta.getId();
        });
    }

    private String token(Long cuentaId, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("alma")
                .subject(String.valueOf(cuentaId))
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder metodo,
            String token, String json) throws Exception {
        return mockMvc.perform(metodo.header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** POST de un recordatorio que tiene que salir bien; devuelve el cuerpo. */
    private String crear(String token, String ruta, String json) throws Exception {
        return enviar(post(BASE + ruta), token, json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static long idDe(String cuerpo) {
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private ResultActions agenda(String token, String desde, String hasta, String tipo) throws Exception {
        String url = BASE + "/agenda?desde=" + desde + "&hasta=" + hasta + (tipo == null ? "" : "&tipo=" + tipo);
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private long primeraNotificacion(String token) throws Exception {
        String cuerpo = agenda(token, "2026-10-28", "2026-11-30", null).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$[0].notificacionId")).longValue();
    }

    private long notificacionDeTipo(String token, String tipo) throws Exception {
        String cuerpo = agenda(token, "2026-10-28", "2026-11-30", tipo).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$[0].notificacionId")).longValue();
    }

    /** Notificación puesta a mano, para casos que la API no genera (por ejemplo, una que ya pasó). */
    private void guardarNotificacion(long recordatorioId, LocalDateTime momento, EstadoNotificacion estado) {
        NotificacionRecordatorio notificacion = new NotificacionRecordatorio(
                recordatorioRepository.findById(recordatorioId).orElseThrow(), momento);
        notificacion.setEstado(estado);
        if (estado == EstadoNotificacion.TOMADA) {
            notificacion.setFechaHoraToma(momento);
        }
        notificacionRepository.save(notificacion);
    }

    /** Una medicación que ya empezó antes de "hoy": la API no deja crearla, pero puede existir. */
    private long medicacionConInicioPasado(LocalDate inicio) {
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuentaA).orElseThrow();
        RecordatorioMedicacion medicacion = new RecordatorioMedicacion();
        medicacion.setPersonaMayor(persona);
        medicacion.setFechaCreacion(AHORA.minusDays(8));
        medicacion.setNombreMedicamento("Enalapril");
        medicacion.setDosis("1 pastilla");
        medicacion.setHoraToma(java.time.LocalTime.of(9, 0));
        medicacion.setFrecuencia(FrecuenciaToma.DIARIA);
        medicacion.setFechaInicio(inicio);
        medicacion.setDuracionDias(31);
        return recordatorioRepository.save(medicacion).getId();
    }

    /** Medicación estándar: Enalapril, 1 pastilla, 09:00. */
    private static String medicacion(String frecuencia, int dias, String fechaInicio) {
        return """
                {"nombreMedicamento":"Enalapril","dosis":"1 pastilla","horaToma":"09:00","frecuencia":"%s",\
                "duracionDias":%d%s}"""
                .formatted(frecuencia, dias, fechaInicio == null ? "" : ",\"fechaInicio\":\"" + fechaInicio + "\"");
    }

    private static String cita(String fechaHora) {
        return """
                {"titulo":"Control anual","especialidad":"Cardiología","medico":"Dra. Pérez",\
                "lugar":"Consultorio Centro","fechaHora":"%s","indicacionesPrevias":"Llevar los estudios."}"""
                .formatted(fechaHora);
    }
}
