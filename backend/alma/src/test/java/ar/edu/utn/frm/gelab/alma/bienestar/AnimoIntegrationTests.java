package ar.edu.utn.frm.gelab.alma.bienestar;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.bienestar.domain.NivelAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.domain.RegistroAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.repository.RegistroAnimoRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Pruebas del contrato de ánimo (sección 12). Usan el Postgres de Docker ("docker compose up -d").
 * Crean sus propias dos cuentas (prefijo test_animo_, sin el seed) y las borran al terminar. El reloj
 * de la aplicación es de prueba: arranca el 04/11/2026 a las 10:00 (hora de Argentina) y cada prueba
 * puede avanzarlo.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(AnimoIntegrationTests.RelojDePrueba.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnimoIntegrationTests {

    private static final String BASE = "/api/animo";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 4, 10, 0);
    private static final String USUARIO_A = "test_animo_a";
    private static final String USUARIO_B = "test_animo_b";

    /** Reloj con la zona de Argentina cuyo "ahora" se puede mover desde la prueba. */
    static class RelojAjustable extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>();

        void fijar(LocalDateTime fechaHora) {
            instante.set(fechaHora.atZone(ZONA).toInstant());
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante.get(), zone);
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    @TestConfiguration
    static class RelojDePrueba {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
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
    private RegistroAnimoRepository registroRepository;
    @Autowired
    private RelojAjustable reloj;

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
    void reiniciarReloj() {
        reloj.fijar(INICIO);
    }

    @BeforeEach
    @AfterEach
    void limpiarRegistros() {
        jdbc.update("delete from registro_animo where persona_mayor_id in "
                + "(select p.id from persona_mayor p where p.cuenta_id in (?, ?))", cuentaA, cuentaB);
    }

    @AfterAll
    void borrarCuentas() {
        limpiarRegistros();
        for (String usuario : List.of(USUARIO_A, USUARIO_B)) {
            cuentaRepository.findByNombreUsuario(usuario).ifPresent(cuenta -> {
                personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(personaMayorRepository::delete);
                cuentaRepository.delete(cuenta);
            });
        }
    }

    // ---------- Acceso ----------

    @Test
    void sinTokenDevuelve401EnLasTresRutas() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"nivelAnimo\":\"BIEN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{\"nivelAnimo\":\"BIEN\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/hoy")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorDevuelve403EnLasTresRutas() throws Exception {
        enviar(post(BASE), tokenAdmin, "{\"nivelAnimo\":\"BIEN\"}").andExpect(status().isForbidden());
        enviar(put(BASE + "/1"), tokenAdmin, "{\"nivelAnimo\":\"BIEN\"}").andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/hoy").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cuentaSinPersonaMayorDevuelve404AlRegistrar() throws Exception {
        enviar(post(BASE), token(999_998L, "PERSONA_MAYOR"), "{\"nivelAnimo\":\"BIEN\"}")
                .andExpect(status().isNotFound());
    }

    // ---------- POST ----------

    @Test
    void registrarBienDevuelve201ConLaPreguntaYGuardaUnRegistro() throws Exception {
        enviar(post(BASE), tokenA, "{\"nivelAnimo\":\"BIEN\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nivelAnimo").value("BIEN"))
                .andExpect(jsonPath("$.fechaHora").value("2026-11-04T10:00:00"))
                .andExpect(jsonPath("$.preguntaContextual").value(MensajesAnimo.PREGUNTA_BIEN));

        assertEquals(1, cantidadDeRegistros(cuentaA));
        assertEquals(0, cantidadDeRegistros(cuentaB));
    }

    @Test
    void cadaNivelDevuelveSuPregunta() throws Exception {
        for (NivelAnimo nivel : NivelAnimo.values()) {
            enviar(post(BASE), tokenA, "{\"nivelAnimo\":\"" + nivel + "\"}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nivelAnimo").value(nivel.name()))
                    .andExpect(jsonPath("$.preguntaContextual").value(MensajesAnimo.preguntaDe(nivel)));
        }
        assertEquals(5, cantidadDeRegistros(cuentaA));
    }

    @Test
    void registrarSinNivelDevuelve400ConElMensajeDelCampo() throws Exception {
        enviar(post(BASE), tokenA, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nivelAnimo"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Elegí cómo te sentís."));
        assertEquals(0, cantidadDeRegistros(cuentaA));
    }

    @Test
    void registrarConValorInvalidoOCuerpoVacioDevuelve400() throws Exception {
        enviar(post(BASE), tokenA, "{\"nivelAnimo\":\"FELIZ\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."));
        enviar(post(BASE), tokenA, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."));
        assertEquals(0, cantidadDeRegistros(cuentaA));
    }

    @Test
    void dosPostSeguidosCreanDosRegistros() throws Exception {
        Long primero = idDe(registrar(tokenA, "BIEN"));
        Long segundo = idDe(registrar(tokenA, "BIEN"));

        assertEquals(2, cantidadDeRegistros(cuentaA));
        assertEquals(false, primero.equals(segundo));
    }

    // ---------- PUT ----------

    @Test
    void modificarCambiaNivelYFechaHoraYConservaElId() throws Exception {
        Long id = idDe(registrar(tokenA, "BIEN"));
        reloj.fijar(INICIO.plusMinutes(5));

        enviar(put(BASE + "/" + id), tokenA, "{\"nivelAnimo\":\"MAL\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nivelAnimo").value("MAL"))
                .andExpect(jsonPath("$.fechaHora").value("2026-11-04T10:05:00"))
                .andExpect(jsonPath("$.preguntaContextual").value(MensajesAnimo.PREGUNTA_MAL));

        assertEquals(1, cantidadDeRegistros(cuentaA));
        hoy(tokenA).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nivelAnimo").value("MAL"));
    }

    @Test
    void modificarConElMismoNivelNoCambiaNada() throws Exception {
        Long id = idDe(registrar(tokenA, "BIEN"));
        reloj.fijar(INICIO.plusMinutes(5));

        enviar(put(BASE + "/" + id), tokenA, "{\"nivelAnimo\":\"BIEN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nivelAnimo").value("BIEN"))
                .andExpect(jsonPath("$.fechaHora").value("2026-11-04T10:00:00"));
        assertEquals(INICIO, registroRepository.findById(id).orElseThrow().getFechaHora());
    }

    @Test
    void modificarConNivelFaltanteOInvalidoDevuelve400() throws Exception {
        Long id = idDe(registrar(tokenA, "BIEN"));

        enviar(put(BASE + "/" + id), tokenA, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value("Elegí cómo te sentís."));
        enviar(put(BASE + "/" + id), tokenA, "{\"nivelAnimo\":\"FELIZ\"}")
                .andExpect(status().isBadRequest());
        assertEquals(NivelAnimo.BIEN, registroRepository.findById(id).orElseThrow().getNivelAnimo());
    }

    @Test
    void modificarRegistroDeOtraPersonaOInexistenteDevuelve404() throws Exception {
        Long idDeA = idDe(registrar(tokenA, "BIEN"));

        enviar(put(BASE + "/" + idDeA), tokenB, "{\"nivelAnimo\":\"MAL\"}").andExpect(status().isNotFound());
        enviar(put(BASE + "/999999999"), tokenA, "{\"nivelAnimo\":\"MAL\"}").andExpect(status().isNotFound());
        assertEquals(NivelAnimo.BIEN, registroRepository.findById(idDeA).orElseThrow().getNivelAnimo());
    }

    @Test
    void modificarRegistroDeAyerDevuelve404() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 11, 3, 23, 59));
        Long id = idDe(registrar(tokenA, "BIEN"));
        reloj.fijar(INICIO);

        enviar(put(BASE + "/" + id), tokenA, "{\"nivelAnimo\":\"MAL\"}").andExpect(status().isNotFound());

        RegistroAnimo registro = registroRepository.findById(id).orElseThrow();
        assertEquals(NivelAnimo.BIEN, registro.getNivelAnimo());
        assertEquals(LocalDateTime.of(2026, 11, 3, 23, 59), registro.getFechaHora());
    }

    @Test
    void modificarUnRegistroDeHoyQueYaNoEsElMasRecienteDevuelve404() throws Exception {
        Long viejo = idDe(registrar(tokenA, "BIEN"));
        reloj.fijar(INICIO.plusHours(1));
        registrar(tokenA, "MAL");

        enviar(put(BASE + "/" + viejo), tokenA, "{\"nivelAnimo\":\"MUY_BIEN\"}").andExpect(status().isNotFound());

        RegistroAnimo registro = registroRepository.findById(viejo).orElseThrow();
        assertEquals(NivelAnimo.BIEN, registro.getNivelAnimo());
        assertEquals(INICIO, registro.getFechaHora());
    }

    // ---------- GET /hoy ----------

    @Test
    void hoySinRegistrosDevuelve204SinCuerpo() throws Exception {
        hoy(tokenA).andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @Test
    void hoyConVariosRegistrosDevuelveElMasReciente() throws Exception {
        registrar(tokenA, "BIEN");
        reloj.fijar(INICIO.plusHours(2));
        Long ultimo = idDe(registrar(tokenA, "NEUTRO"));

        hoy(tokenA).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ultimo))
                .andExpect(jsonPath("$.nivelAnimo").value("NEUTRO"))
                .andExpect(jsonPath("$.fechaHora").value("2026-11-04T12:00:00"))
                .andExpect(jsonPath("$.preguntaContextual").value(MensajesAnimo.PREGUNTA_NEUTRO));
    }

    @Test
    void conLaMismaHoraGanaElDeMayorId() throws Exception {
        registrar(tokenA, "BIEN");
        Long ultimo = idDe(registrar(tokenA, "MAL"));

        hoy(tokenA).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ultimo));
    }

    @Test
    void unRegistroDeAyerNoCuentaComoAnimoDeHoy() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 11, 3, 23, 59));
        registrar(tokenA, "BIEN");
        reloj.fijar(LocalDateTime.of(2026, 11, 4, 0, 0));

        hoy(tokenA).andExpect(status().isNoContent());
    }

    @Test
    void unRegistroDeLasCeroHorasEsDeHoy() throws Exception {
        reloj.fijar(LocalDateTime.of(2026, 11, 4, 0, 0));
        Long id = idDe(registrar(tokenA, "BIEN"));

        hoy(tokenA).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void cadaPersonaVeSoloLoSuyo() throws Exception {
        registrar(tokenA, "MUY_BIEN");

        hoy(tokenB).andExpect(status().isNoContent());

        Long idDeB = idDe(registrar(tokenB, "MUY_MAL"));
        hoy(tokenA).andExpect(jsonPath("$.nivelAnimo").value("MUY_BIEN"));
        hoy(tokenB).andExpect(jsonPath("$.id").value(idDeB)).andExpect(jsonPath("$.nivelAnimo").value("MUY_MAL"));
    }

    @Test
    void fechaHoraQuedaEnHoraDeArgentina() throws Exception {
        // 22:30 en Argentina es 01:30 del día siguiente en UTC: el registro tiene que guardar la hora local.
        reloj.fijar(LocalDateTime.of(2026, 11, 4, 22, 30));
        Long id = idDe(registrar(tokenA, "BIEN"));

        assertEquals(LocalDateTime.of(2026, 11, 4, 22, 30), registroRepository.findById(id).orElseThrow().getFechaHora());
        hoy(tokenA).andExpect(status().isOk()).andExpect(jsonPath("$.fechaHora").value("2026-11-04T22:30:00"));
    }

    // ---------- Ayudas ----------

    private String registrar(String token, String nivel) throws Exception {
        return enviar(post(BASE), token, "{\"nivelAnimo\":\"" + nivel + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static Long idDe(String cuerpo) {
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private ResultActions hoy(String token) throws Exception {
        return mockMvc.perform(get(BASE + "/hoy").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private long cantidadDeRegistros(Long cuentaId) {
        return jdbc.queryForObject("select count(*) from registro_animo r join persona_mayor p "
                + "on p.id = r.persona_mayor_id where p.cuenta_id = ?", Long.class, cuentaId);
    }

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
            persona.setFechaNacimiento(LocalDate.of(1950, 5, 12));
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

    private ResultActions enviar(MockHttpServletRequestBuilder metodo, String token, String json) throws Exception {
        return mockMvc.perform(metodo.header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
