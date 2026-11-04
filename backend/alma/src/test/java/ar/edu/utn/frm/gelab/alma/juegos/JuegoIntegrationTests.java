package ar.edu.utn.frm.gelab.alma.juegos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;
import ar.edu.utn.frm.gelab.alma.juegos.domain.IntentoEjercicio;
import ar.edu.utn.frm.gelab.alma.juegos.repository.IntentoEjercicioRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Pruebas del contrato de juegos (sección 13). Usan el Postgres de Docker ("docker compose up -d").
 * No tocan el catálogo real ni a ana y carlos: crean sus propias dos cuentas y sus propios ejercicios
 * (consigna "TEST JUEGO …") y los borran después de cada prueba. Responden siempre ejercicios propios,
 * así no dependen de cuál elige el selector. El reloj queda fijo en el 04/11/2026 a las 10:00.
 * El caso "sin ejercicios activos" se prueba en JuegoServiceTest, sin base.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(JuegoIntegrationTests.RelojFijo.class)
class JuegoIntegrationTests {

    private static final String BASE = "/api/juegos";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 10, 0);
    private static final String PREFIJO = "TEST JUEGO ";
    private static final String USUARIO_A = "test_juego_a";
    private static final String USUARIO_B = "test_juego_b";

    /** Reloj fijo: "ahora" es el 04/11/2026 a las 10:00. */
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
    private EjercicioRepository ejercicioRepository;
    @Autowired
    private IntentoEjercicioRepository intentoRepository;

    private Long cuentaA;
    private Long cuentaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void crearCuentas() {
        cuentaA = cuentaDePrueba(USUARIO_A, "Test", "A");
        cuentaB = cuentaDePrueba(USUARIO_B, "Test", "B");
        tokenA = token(cuentaA, "PERSONA_MAYOR");
        tokenB = token(cuentaB, "PERSONA_MAYOR");
        tokenAdmin = token(999_999L, "ADMINISTRADOR");
    }

    /** Orden por las claves foráneas: intentos, progresos, ejercicios de prueba, personas y cuentas. */
    @AfterEach
    void limpiar() {
        String personas = "select p.id from persona_mayor p join cuenta c on c.id = p.cuenta_id "
                + "where c.nombre_usuario in ('" + USUARIO_A + "', '" + USUARIO_B + "')";
        String ejercicios = "select e.id from ejercicio e where e.consigna like '" + PREFIJO + "%'";
        jdbc.update("delete from intento_ejercicio where persona_mayor_id in (" + personas + ")"
                + " or ejercicio_id in (" + ejercicios + ")");
        jdbc.update("delete from progreso_juego where persona_mayor_id in (" + personas + ")"
                + " or ultimo_ejercicio_id in (" + ejercicios + ")");
        jdbc.update("delete from opcion_respuesta where ejercicio_id in (" + ejercicios + ")");
        jdbc.update("delete from ejercicio where consigna like '" + PREFIJO + "%'");
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
        mockMvc.perform(get(BASE + "/siguiente"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
        mockMvc.perform(post(BASE + "/respuesta").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorDevuelve403() throws Exception {
        mockMvc.perform(get(BASE + "/siguiente").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        Long id = opcionMultiple(NivelDificultad.BASICO);
        responder(tokenAdmin, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                .andExpect(status().isForbidden());
    }

    // --- Siguiente (6.1) ---

    @Test
    void siguienteNoDevuelveRespuestasNiNivelYNoGuardaNada() throws Exception {
        opcionMultiple(NivelDificultad.BASICO);
        ordenar(NivelDificultad.BASICO);

        String cuerpo = mockMvc.perform(get(BASE + "/siguiente").header(HttpHeaders.AUTHORIZATION, bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejercicio.id").exists())
                .andExpect(jsonPath("$.ejercicio.opciones[0].id").exists())
                .andExpect(jsonPath("$.ejercicio.opciones[0].texto").exists())
                .andExpect(jsonPath("$.posicion").value(1))
                .andExpect(jsonPath("$.totalRonda").value(10))
                .andReturn().getResponse().getContentAsString();

        // Nombres de campo del JSON (con la comilla), para no confundirse con el texto de una consigna.
        assertThat(cuerpo).doesNotContain("\"esCorrecta\"", "\"ordenCorrecto\"", "\"explicacion\"", "\"nivel");
        assertThat(intentos(cuentaA)).isZero();
        assertThat(progreso(cuentaA)).isNull();
    }

    // --- Responder opción múltiple (5.4) ---

    @Test
    void respuestaCorrectaGuardaElIntentoYCreaElProgreso() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);

        responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(true))
                .andExpect(jsonPath("$.explicacion").value("Explicación de prueba."))
                .andExpect(jsonPath("$.respuestaCorrecta", contains("Correcta")))
                .andExpect(jsonPath("$.rondaCompleta").value(false))
                .andExpect(jsonPath("$.jugadosHoy").value(1));

        assertThat(intentos(cuentaA)).isEqualTo(1);
        Map<String, Object> progreso = progreso(cuentaA);
        assertThat(progreso).isNotNull();
        assertThat(progreso.get("nivel_actual")).isEqualTo("BASICO");
        assertThat(progreso.get("aciertos_consecutivos")).isEqualTo(1);
        assertThat(progreso.get("errores_consecutivos")).isEqualTo(0);
        assertThat(((Number) progreso.get("ultimo_ejercicio_id")).longValue()).isEqualTo(id);
        assertThat(progreso.get("fecha_ultima_sesion").toString()).isEqualTo("2026-11-04");
    }

    @Test
    void respuestaIncorrectaDevuelveLaCorrecta() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);

        responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + incorrecta(id) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(false))
                .andExpect(jsonPath("$.respuestaCorrecta", contains("Correcta")))
                .andExpect(jsonPath("$.jugadosHoy").value(1));

        assertThat(progreso(cuentaA).get("errores_consecutivos")).isEqualTo(1);
    }

    @Test
    void opcionMultipleSinOpcionODeOtroEjercicioDevuelve400SinRegistrar() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);
        Long otro = opcionMultiple(NivelDificultad.BASICO);

        for (String cuerpo : List.of(
                "{\"ejercicioId\":" + id + "}",
                "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(otro) + "}",
                "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + ",\"ordenOpcionIds\":[1]}")) {
            responder(tokenA, cuerpo)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                    .andExpect(jsonPath("$.errores", hasSize(1)))
                    .andExpect(jsonPath("$.errores[0].campo").value("opcionId"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value("Elegí una opción para responder."));
        }
        assertThat(intentos(cuentaA)).isZero();
        assertThat(progreso(cuentaA)).isNull();
    }

    @Test
    void sinEjercicioIdDevuelve400() throws Exception {
        responder(tokenA, "{\"opcionId\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores[0].campo").value("ejercicioId"));
    }

    @Test
    void ejercicioInexistenteDevuelve404() throws Exception {
        responder(tokenA, "{\"ejercicioId\":" + Long.MAX_VALUE + ",\"opcionId\":1}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos lo que buscás."));
        assertThat(intentos(cuentaA)).isZero();
    }

    @Test
    void ejercicioDadoDeBajaMientrasSeJugabaSeAceptaIgual() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);
        jdbc.update("update ejercicio set fecha_baja = ? where id = ?", LocalDate.of(2026, 11, 4), id);

        responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(true));
    }

    // --- Ordenar (5.4 y 5.6) ---

    @Test
    void ordenarCompletoYCorrectoAcierta() throws Exception {
        Long id = ordenar(NivelDificultad.BASICO);
        List<Long> orden = ordenCorrecto(id);

        responder(tokenA, ordenJson(id, orden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(true))
                .andExpect(jsonPath("$.respuestaCorrecta", contains("Paso 1", "Paso 2", "Paso 3", "Paso 4")));
    }

    @Test
    void ordenarConUnParInvertidoEsError() throws Exception {
        Long id = ordenar(NivelDificultad.BASICO);
        List<Long> orden = new ArrayList<>(ordenCorrecto(id));
        Long segundo = orden.get(1);
        orden.set(1, orden.get(2));
        orden.set(2, segundo);

        responder(tokenA, ordenJson(id, orden))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(false))
                .andExpect(jsonPath("$.respuestaCorrecta", contains("Paso 1", "Paso 2", "Paso 3", "Paso 4")));
    }

    @Test
    void ordenarConIdsFaltantesRepetidosOAjenosDevuelve400SinRegistrar() throws Exception {
        Long id = ordenar(NivelDificultad.BASICO);
        List<Long> orden = ordenCorrecto(id);
        Long ajeno = correcta(opcionMultiple(NivelDificultad.BASICO));

        List<String> cuerpos = List.of(
                "{\"ejercicioId\":" + id + "}",
                ordenJson(id, orden.subList(0, 3)),
                ordenJson(id, List.of(orden.get(0), orden.get(0), orden.get(2), orden.get(3))),
                ordenJson(id, List.of(orden.get(0), orden.get(1), orden.get(2), ajeno)),
                ordenJson(id, List.of(orden.get(0), orden.get(1), orden.get(2), orden.get(3), orden.get(0))),
                "{\"ejercicioId\":" + id + ",\"opcionId\":" + orden.get(0) + ",\"ordenOpcionIds\":"
                        + orden + "}");
        for (String cuerpo : cuerpos) {
            responder(tokenA, cuerpo)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                    .andExpect(jsonPath("$.errores[0].campo").value("ordenOpcionIds"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value("Ordená todos los pasos para responder."));
        }
        assertThat(intentos(cuentaA)).isZero();
    }

    // --- Nivel (5.5) ---

    @Test
    void tresAciertosSubenDeNivelYDosErroresBajan() throws Exception {
        Long basico = opcionMultiple(NivelDificultad.BASICO);
        opcionMultiple(NivelDificultad.INTERMEDIO);

        for (int i = 0; i < 3; i++) {
            responderBien(tokenA, basico);
        }
        Map<String, Object> progreso = progreso(cuentaA);
        assertThat(progreso.get("nivel_actual")).isEqualTo("INTERMEDIO");
        assertThat(progreso.get("aciertos_consecutivos")).isEqualTo(0);

        // Ahora el siguiente sale del nivel intermedio (hay al menos uno: el de la prueba).
        String siguiente = siguiente(tokenA).andReturn().getResponse().getContentAsString();
        Long elegido = ((Number) JsonPath.read(siguiente, "$.ejercicio.id")).longValue();
        assertThat(jdbc.queryForObject("select nivel_dificultad from ejercicio where id = ?", String.class, elegido))
                .isEqualTo("INTERMEDIO");

        responderMal(tokenA, basico);
        assertThat(progreso(cuentaA).get("nivel_actual")).isEqualTo("INTERMEDIO");
        responderMal(tokenA, basico);
        progreso = progreso(cuentaA);
        assertThat(progreso.get("nivel_actual")).isEqualTo("BASICO");
        assertThat(progreso.get("errores_consecutivos")).isEqualTo(0);
    }

    // --- Ronda (5.7) ---

    @Test
    void elDecimoIntentoCompletaLaRondaYLaPosicionVuelveA1() throws Exception {
        opcionMultiple(NivelDificultad.BASICO);
        Long id = opcionMultiple(NivelDificultad.BASICO);

        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(1));
        for (int i = 1; i <= 9; i++) {
            responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                    .andExpect(jsonPath("$.rondaCompleta").value(false))
                    .andExpect(jsonPath("$.jugadosHoy").value(i));
        }
        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(10));

        responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + incorrecta(id) + "}")
                .andExpect(jsonPath("$.rondaCompleta").value(true))
                .andExpect(jsonPath("$.jugadosHoy").value(10));
        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(1));

        responderBien(tokenA, id);
        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(2));
    }

    @Test
    void losIntentosDeAyerNoCuentanParaLaRonda() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuentaA).orElseThrow();
        Ejercicio ejercicio = ejercicioRepository.findById(id).orElseThrow();
        for (LocalDateTime cuando : List.of(LocalDateTime.of(2026, 11, 3, 23, 59), LocalDateTime.of(2026, 11, 5, 0, 0))) {
            IntentoEjercicio intento = new IntentoEjercicio();
            intento.setPersonaMayor(persona);
            intento.setEjercicio(ejercicio);
            intento.setFechaHora(cuando);
            intento.setAcierto(true);
            intentoRepository.save(intento);
        }

        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(1));
        responder(tokenA, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                .andExpect(jsonPath("$.jugadosHoy").value(1));
    }

    // --- Dueño del dato ---

    @Test
    void unaPersonaNoVeNiAlteraElProgresoDeOtra() throws Exception {
        Long id = opcionMultiple(NivelDificultad.BASICO);
        opcionMultiple(NivelDificultad.INTERMEDIO);
        for (int i = 0; i < 3; i++) {
            responderBien(tokenA, id);
        }

        // B arranca de cero: su ronda está en 1 y no tiene progreso.
        siguiente(tokenB).andExpect(jsonPath("$.posicion").value(1));
        assertThat(progreso(cuentaB)).isNull();

        responder(tokenB, "{\"ejercicioId\":" + id + ",\"opcionId\":" + incorrecta(id) + "}")
                .andExpect(jsonPath("$.jugadosHoy").value(1));

        Map<String, Object> deA = progreso(cuentaA);
        assertThat(deA.get("nivel_actual")).isEqualTo("INTERMEDIO");
        assertThat(deA.get("errores_consecutivos")).isEqualTo(0);
        assertThat(intentos(cuentaA)).isEqualTo(3);
        assertThat(progreso(cuentaB).get("nivel_actual")).isEqualTo("BASICO");
        assertThat(progreso(cuentaB).get("errores_consecutivos")).isEqualTo(1);
        siguiente(tokenA).andExpect(jsonPath("$.posicion").value(4));
    }

    // --- Ayudas ---

    private ResultActions siguiente(String token) throws Exception {
        return mockMvc.perform(get(BASE + "/siguiente").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private ResultActions responder(String token, String json) throws Exception {
        return mockMvc.perform(post(BASE + "/respuesta").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void responderBien(String token, Long id) throws Exception {
        responder(token, "{\"ejercicioId\":" + id + ",\"opcionId\":" + correcta(id) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(true));
    }

    private void responderMal(String token, Long id) throws Exception {
        responder(token, "{\"ejercicioId\":" + id + ",\"opcionId\":" + incorrecta(id) + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acierto").value(false));
    }

    private static String ordenJson(Long id, List<Long> orden) {
        return "{\"ejercicioId\":" + id + ",\"ordenOpcionIds\":" + orden + "}";
    }

    /** Ejercicio de opción múltiple propio: "Correcta" y dos incorrectas. Devuelve el id. */
    private Long opcionMultiple(NivelDificultad nivel) {
        Ejercicio ejercicio = ejercicio(FormatoEjercicio.OPCION_MULTIPLE, nivel);
        ejercicio.getOpciones().add(opcion("Correcta", true, null));
        ejercicio.getOpciones().add(opcion("Incorrecta 1", false, null));
        ejercicio.getOpciones().add(opcion("Incorrecta 2", false, null));
        return ejercicioRepository.save(ejercicio).getId();
    }

    /** Ejercicio de ordenar propio, con los pasos "Paso 1" a "Paso 4". Devuelve el id. */
    private Long ordenar(NivelDificultad nivel) {
        Ejercicio ejercicio = ejercicio(FormatoEjercicio.ORDENAR, nivel);
        // Se guardan desordenados, para que el orden correcto no coincida con el de los ids.
        for (int paso : List.of(3, 1, 4, 2)) {
            ejercicio.getOpciones().add(opcion("Paso " + paso, false, paso));
        }
        return ejercicioRepository.save(ejercicio).getId();
    }

    private static Ejercicio ejercicio(FormatoEjercicio formato, NivelDificultad nivel) {
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setCategoria(CategoriaEjercicio.SERIE);
        ejercicio.setFormato(formato);
        ejercicio.setNivelDificultad(nivel);
        ejercicio.setConsigna(PREFIJO + UUID.randomUUID());
        ejercicio.setExplicacion("Explicación de prueba.");
        ejercicio.setFechaAlta(LocalDate.of(2026, 11, 4));
        return ejercicio;
    }

    private static OpcionRespuesta opcion(String texto, boolean esCorrecta, Integer ordenCorrecto) {
        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setTexto(texto);
        opcion.setEsCorrecta(esCorrecta);
        opcion.setOrdenCorrecto(ordenCorrecto);
        return opcion;
    }

    private Long correcta(Long ejercicioId) {
        return jdbc.queryForObject("select id from opcion_respuesta where ejercicio_id = ? and es_correcta",
                Long.class, ejercicioId);
    }

    private Long incorrecta(Long ejercicioId) {
        return jdbc.queryForObject(
                "select id from opcion_respuesta where ejercicio_id = ? and not es_correcta order by id limit 1",
                Long.class, ejercicioId);
    }

    private List<Long> ordenCorrecto(Long ejercicioId) {
        return jdbc.queryForList("select id from opcion_respuesta where ejercicio_id = ? order by orden_correcto",
                Long.class, ejercicioId);
    }

    private long intentos(Long cuentaId) {
        return jdbc.queryForObject("select count(*) from intento_ejercicio i join persona_mayor p "
                + "on p.id = i.persona_mayor_id where p.cuenta_id = ?", Long.class, cuentaId);
    }

    /** Fila de progreso_juego de la persona, o null si no tiene. */
    private Map<String, Object> progreso(Long cuentaId) {
        List<Map<String, Object>> filas = jdbc.queryForList("select g.* from progreso_juego g join persona_mayor p "
                + "on p.id = g.persona_mayor_id where p.cuenta_id = ?", cuentaId);
        return filas.isEmpty() ? null : filas.getFirst();
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
