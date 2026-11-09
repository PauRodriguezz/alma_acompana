package ar.edu.utn.frm.gelab.alma.microcapsulas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Pruebas del contrato de microcápsulas (sección 13). Usan el Postgres de Docker ("docker compose up -d").
 * No tocan el catálogo real ni a ana y carlos: crean sus propias dos cuentas y sus propias microcápsulas
 * (título "TEST MICRO …") y las borran después de cada prueba. Como la base tiene otras microcápsulas
 * vigentes, el orden y "guardada" del listado se comprueban solo sobre las "TEST MICRO".
 * El reloj se puede mover: arranca el 04/11/2026 a las 10:00 en cada prueba.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(LecturaMicrocapsulaIntegrationTests.ConfigReloj.class)
class LecturaMicrocapsulaIntegrationTests {

    private static final String BASE = "/api/microcapsulas";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 10, 0);
    private static final String PREFIJO = "TEST MICRO ";
    private static final String USUARIO_A = "test_micro_a";
    private static final String USUARIO_B = "test_micro_b";
    private static final long INEXISTENTE = 999_999_999L;
    private static final String NO_ENCONTRADO = "No encontramos lo que buscás.";

    /** Reloj que la prueba puede mover. Solo existe en el contexto de esta clase. */
    static class RelojMovil extends Clock {

        private volatile Instant instante;

        void poner(LocalDateTime fechaHora) {
            instante = fechaHora.atZone(ZONA).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return Clock.fixed(instante, zona);
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @TestConfiguration
    static class ConfigReloj {
        @Bean
        @Primary
        RelojMovil relojMovil() {
            RelojMovil reloj = new RelojMovil();
            reloj.poner(AHORA);
            return reloj;
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
    private MicrocapsulaRepository microcapsulaRepository;
    @Autowired
    private RelojMovil reloj;

    private Long cuentaA;
    private Long cuentaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        reloj.poner(AHORA);
        cuentaA = cuentaDePrueba(USUARIO_A, "Test", "A");
        cuentaB = cuentaDePrueba(USUARIO_B, "Test", "B");
        tokenA = token(cuentaA, "PERSONA_MAYOR");
        tokenB = token(cuentaB, "PERSONA_MAYOR");
        tokenAdmin = token(999_999L, "ADMINISTRADOR");
    }

    /** Orden por las claves foráneas: guardadas, microcápsulas de prueba, personas y cuentas. */
    @AfterEach
    void limpiar() {
        String personas = "select p.id from persona_mayor p join cuenta c on c.id = p.cuenta_id "
                + "where c.nombre_usuario in ('" + USUARIO_A + "', '" + USUARIO_B + "')";
        String microcapsulas = "select m.id from microcapsula m where m.titulo like '" + PREFIJO + "%'";
        jdbc.update("delete from microcapsula_guardada where persona_mayor_id in (" + personas + ")"
                + " or microcapsula_id in (" + microcapsulas + ")");
        jdbc.update("delete from microcapsula where titulo like '" + PREFIJO + "%'");
        for (String usuario : List.of(USUARIO_A, USUARIO_B)) {
            cuentaRepository.findByNombreUsuario(usuario).ifPresent(cuenta -> {
                personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(personaMayorRepository::delete);
                cuentaRepository.delete(cuenta);
            });
        }
    }

    // --- Acceso (sección 7) ---

    @Test
    void sinTokenDevuelve401() throws Exception {
        Long id = microcapsula("Acceso", LocalDate.of(2026, 9, 1));
        for (MockHttpServletRequestBuilder pedido : List.of(get(BASE), get(BASE + "/guardadas"),
                get(BASE + "/" + id), put(BASE + "/" + id + "/guardada"), delete(BASE + "/" + id + "/guardada"))) {
            mockMvc.perform(pedido)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
        }
    }

    @Test
    void administradorDevuelve403() throws Exception {
        Long id = microcapsula("Acceso", LocalDate.of(2026, 9, 1));
        for (MockHttpServletRequestBuilder pedido : List.of(get(BASE), get(BASE + "/guardadas"),
                get(BASE + "/" + id), put(BASE + "/" + id + "/guardada"), delete(BASE + "/" + id + "/guardada"))) {
            mockMvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        }
        assertThat(guardadasEnBase()).isZero();
    }

    // --- Listado (5.1) ---

    @Test
    void listadoTraeSoloVigentesEnOrdenYConGuardadaPorPersona() throws Exception {
        Long primera = microcapsula("Primera", LocalDate.of(2026, 9, 1));
        Long segunda = microcapsula("Segunda", LocalDate.of(2026, 9, 2));
        // Misma fecha: desempata el id, la creada después va antes.
        Long empateViejo = microcapsula("Empate viejo", LocalDate.of(2026, 9, 3));
        Long empateNuevo = microcapsula("Empate nuevo", LocalDate.of(2026, 9, 3));
        darDeBaja(microcapsula("Dada de baja", LocalDate.of(2026, 9, 4)));
        guardar(tokenA, segunda).andExpect(status().isNoContent());

        List<Map<String, Object>> deA = deTest(listado(tokenA));
        assertThat(deA).extracting(m -> ((Number) m.get("id")).longValue())
                .containsExactly(empateNuevo, empateViejo, segunda, primera);
        assertThat(deA).extracting(m -> m.get("guardada")).containsExactly(false, false, true, false);
        assertThat(deA.get(2)).containsOnlyKeys("id", "titulo", "categoria", "imagen", "guardada")
                .containsEntry("titulo", PREFIJO + "Segunda")
                .containsEntry("categoria", "PREVENCION_CAIDAS");

        // Lo que guardó A no aparece como guardado para B.
        assertThat(deTest(listado(tokenB))).extracting(m -> m.get("guardada"))
                .containsExactly(false, false, false, false);
    }

    @Test
    void unaDadaDeBajaNoApareceEnElListadoAunqueEsteGuardada() throws Exception {
        Long id = microcapsula("Se da de baja", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());
        darDeBaja(id);

        assertThat(deTest(listado(tokenA))).isEmpty();
    }

    // --- Guardadas (5.2) ---

    @Test
    void guardadasIncluyeLasDadasDeBajaDeLaUltimaGuardadaALaPrimera() throws Exception {
        Long vieja = microcapsula("Guardada a las 10:05", LocalDate.of(2026, 9, 1));
        Long nueva = microcapsula("Guardada a las 10:00", LocalDate.of(2026, 9, 2));
        microcapsula("Sin guardar", LocalDate.of(2026, 9, 3));

        // La de id menor se guarda más tarde: tiene que salir primero por fechaGuardado, no por id.
        reloj.poner(AHORA.plusMinutes(5));
        guardar(tokenA, vieja).andExpect(status().isNoContent());
        reloj.poner(AHORA);
        guardar(tokenA, nueva).andExpect(status().isNoContent());
        darDeBaja(vieja);

        mockMvc.perform(get(BASE + "/guardadas").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(vieja))
                .andExpect(jsonPath("$[0].guardada").value(true))
                .andExpect(jsonPath("$[0].fechaPublicacion").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(nueva))
                .andExpect(jsonPath("$[1].guardada").value(true));

        assertThat(jdbc.queryForObject("select fecha_guardado from microcapsula_guardada where microcapsula_id = ?",
                LocalDateTime.class, vieja)).isEqualTo(AHORA.plusMinutes(5));
    }

    @Test
    void guardadasNoChocaConLaRutaDelDetalle() throws Exception {
        mockMvc.perform(get(BASE + "/guardadas").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Detalle (5.3) ---

    @Test
    void detalleDeUnaVigenteDevuelve200() throws Exception {
        Long id = microcapsula("Vigente", LocalDate.of(2026, 9, 1));

        detalle(tokenA, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value(PREFIJO + "Vigente"))
                .andExpect(jsonPath("$.texto").value("Primer párrafo.\n\nSegundo párrafo."))
                .andExpect(jsonPath("$.categoria").value("PREVENCION_CAIDAS"))
                .andExpect(jsonPath("$.imagen").isEmpty())
                .andExpect(jsonPath("$.guardada").value(false))
                .andExpect(jsonPath("$.fechaPublicacion").doesNotExist())
                .andExpect(jsonPath("$.fechaBaja").doesNotExist());

        guardar(tokenA, id).andExpect(status().isNoContent());
        detalle(tokenA, id).andExpect(jsonPath("$.guardada").value(true));
    }

    @Test
    void detalleDeUnaDadaDeBajaNoGuardadaDevuelve404() throws Exception {
        Long id = microcapsula("Baja sin guardar", LocalDate.of(2026, 9, 1));
        darDeBaja(id);

        detalle(tokenA, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
    }

    @Test
    void detalleDeUnaDadaDeBajaGuardadaDevuelve200() throws Exception {
        Long id = microcapsula("Baja guardada", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());
        darDeBaja(id);

        detalle(tokenA, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.guardada").value(true));
    }

    @Test
    void detalleDeUnaInexistenteDevuelve404() throws Exception {
        detalle(tokenA, INEXISTENTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
    }

    // --- Guardar (5.4) ---

    @Test
    void guardarDosVecesCreaUnaSolaMarca() throws Exception {
        Long id = microcapsula("Guardar", LocalDate.of(2026, 9, 1));

        guardar(tokenA, id).andExpect(status().isNoContent());
        guardar(tokenA, id).andExpect(status().isNoContent());

        assertThat(guardadas(cuentaA, id)).isEqualTo(1);
    }

    @Test
    void guardarUnaDadaDeBajaNoGuardadaDevuelve404() throws Exception {
        Long id = microcapsula("Baja", LocalDate.of(2026, 9, 1));
        darDeBaja(id);

        guardar(tokenA, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
        assertThat(guardadas(cuentaA, id)).isZero();
    }

    @Test
    void guardarDeNuevoUnaDadaDeBajaYaGuardadaNoFalla() throws Exception {
        Long id = microcapsula("Baja ya guardada", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());
        darDeBaja(id);

        guardar(tokenA, id).andExpect(status().isNoContent());
        assertThat(guardadas(cuentaA, id)).isEqualTo(1);
    }

    @Test
    void guardarUnaInexistenteDevuelve404() throws Exception {
        guardar(tokenA, INEXISTENTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
    }

    // --- Quitar (5.5) ---

    @Test
    void quitarBorraLaMarcaYRepetirloDevuelve204() throws Exception {
        Long id = microcapsula("Quitar", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());

        quitar(tokenA, id).andExpect(status().isNoContent());
        assertThat(guardadas(cuentaA, id)).isZero();
        quitar(tokenA, id).andExpect(status().isNoContent());
        assertThat(guardadas(cuentaA, id)).isZero();
    }

    @Test
    void quitarUnaDadaDeBajaGuardadaLaSacaYDespuesNoEsAccesible() throws Exception {
        Long id = microcapsula("Quitar baja", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());
        darDeBaja(id);

        quitar(tokenA, id).andExpect(status().isNoContent());
        assertThat(guardadas(cuentaA, id)).isZero();
        detalle(tokenA, id).andExpect(status().isNotFound());
    }

    @Test
    void quitarUnaInexistenteDevuelve404() throws Exception {
        quitar(tokenA, INEXISTENTE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
    }

    // --- Aislamiento ---

    @Test
    void loQueGuardaUnaPersonaNoCambiaNadaParaLaOtra() throws Exception {
        Long id = microcapsula("Aislamiento", LocalDate.of(2026, 9, 1));
        guardar(tokenA, id).andExpect(status().isNoContent());

        detalle(tokenB, id).andExpect(jsonPath("$.guardada").value(false));
        mockMvc.perform(get(BASE + "/guardadas").header(HttpHeaders.AUTHORIZATION, bearer(tokenB)))
                .andExpect(jsonPath("$", hasSize(0)));

        // Que B quite la suya (que no tiene) no borra la de A.
        quitar(tokenB, id).andExpect(status().isNoContent());
        assertThat(guardadas(cuentaA, id)).isEqualTo(1);

        // Una dada de baja que guardó A no es accesible para B.
        darDeBaja(id);
        detalle(tokenA, id).andExpect(status().isOk());
        detalle(tokenB, id).andExpect(status().isNotFound());
        guardar(tokenB, id).andExpect(status().isNotFound());
    }

    // --- Ayudas ---

    private String listado(String token) throws Exception {
        return mockMvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Solo las microcápsulas de esta prueba, en el orden en que vinieron. */
    private static List<Map<String, Object>> deTest(String cuerpo) {
        List<Map<String, Object>> todas = JsonPath.read(cuerpo, "$[*]");
        return todas.stream().filter(m -> ((String) m.get("titulo")).startsWith(PREFIJO)).toList();
    }

    private ResultActions detalle(String token, Long id) throws Exception {
        return mockMvc.perform(get(BASE + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions guardar(String token, Long id) throws Exception {
        return mockMvc.perform(put(BASE + "/" + id + "/guardada").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions quitar(String token, Long id) throws Exception {
        return mockMvc.perform(delete(BASE + "/" + id + "/guardada").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private Long microcapsula(String titulo, LocalDate fechaPublicacion) {
        Microcapsula microcapsula = new Microcapsula();
        microcapsula.setTitulo(PREFIJO + titulo);
        microcapsula.setTexto("Primer párrafo.\n\nSegundo párrafo.");
        microcapsula.setCategoria(CategoriaMicrocapsula.PREVENCION_CAIDAS);
        microcapsula.setFechaPublicacion(fechaPublicacion);
        return microcapsulaRepository.save(microcapsula).getId();
    }

    /** Baja lógica directa en la base, sin pasar por el panel. */
    private void darDeBaja(Long id) {
        jdbc.update("update microcapsula set fecha_baja = ? where id = ?", AHORA.toLocalDate(), id);
    }

    private long guardadas(Long cuentaId, Long microcapsulaId) {
        return jdbc.queryForObject("select count(*) from microcapsula_guardada g "
                + "join persona_mayor p on p.id = g.persona_mayor_id "
                + "where p.cuenta_id = ? and g.microcapsula_id = ?", Long.class, cuentaId, microcapsulaId);
    }

    /** Guardadas de las dos personas de prueba. */
    private long guardadasEnBase() {
        return jdbc.queryForObject("select count(*) from microcapsula_guardada g "
                + "join persona_mayor p on p.id = g.persona_mayor_id "
                + "where p.cuenta_id in (?, ?)", Long.class, cuentaA, cuentaB);
    }

    private Long cuentaDePrueba(String nombreUsuario, String nombre, String apellido) {
        return cuentaRepository.findByNombreUsuario(nombreUsuario).map(Cuenta::getId).orElseGet(() -> {
            Cuenta cuenta = new Cuenta();
            cuenta.setNombreUsuario(nombreUsuario);
            cuenta.setContrasenia(passwordEncoder.encode("Alma2026!"));
            cuenta.setRol(Rol.PERSONA_MAYOR);
            cuenta.setFechaAlta(LocalDate.of(2026, 11, 4));
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
}
