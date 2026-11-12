package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Endpoints del guion y el progreso (contrato del grafo, secciones 5.1 a 5.3, 6, 7.1 a 7.4 y 11), de punta a
 * punta contra el Postgres y el Neo4j de {@code docker compose up -d}. Cuentas {@code test_historia_*}; el
 * subgrafo de cada persona se borra con {@code eliminarHistoria} antes y después de cada prueba. Seed apagado:
 * nunca se toca la historia de {@code ana}. El reloj se puede mover, para controlar las fechas.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import({ HistoriaGuionIntegrationTests.ConfigReloj.class, ModeloDePruebaConfig.class })
// Contexto propio (reloj movible): se cierra al terminar para liberar su pool de conexiones de PostgreSQL.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HistoriaGuionIntegrationTests {

    private static final String PROXIMA = "/api/historia/guion/proxima";
    private static final String ESTADO = "/api/historia/guion/estado";
    private static final String USUARIO_A = "test_historia_a";
    private static final String USUARIO_B = "test_historia_b";
    private static final String SIN_PERSONA = "test_historia_sin_persona";
    private static final List<String> USUARIOS = List.of(USUARIO_A, USUARIO_B, SIN_PERSONA);
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 4, 10, 0);

    private static final String NO_ENCONTRADO = "No encontramos lo que buscás.";
    private static final String DATOS_INVALIDOS = "Revisá los datos ingresados.";
    private static final String OBLIGATORIA = "Escribí una respuesta o elegí saltear la pregunta.";
    private static final String MUY_LARGA = "Tu respuesta es muy larga. Probá con una más corta.";
    private static final String YA_RESPONDIDA = "Esa pregunta ya fue respondida.";
    private static final String PRIMERA_VEZ = "Me gustaría conocerte mejor. Te voy a hacer preguntas de a una "
            + "sobre tu vida, y podés saltear las que quieras.";
    private static final String DESPUES_DE_SALTEO = "Está bien, pasemos a otra.";
    private static final String SIGAMOS = "Sigamos con otra pregunta.";
    private static final String AHORA_ME_GUSTARIA = "Ahora me gustaría saber otra cosa.";
    private static final String VAMOS_CON_LA_SIGUIENTE = "Vamos con la siguiente.";
    private static final String FINAL = "Ya respondiste todas las preguntas que tengo por ahora. "
            + "¡Gracias por compartir tu historia conmigo!";

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
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private Neo4jClient neo4j;
    @Autowired
    private RelojMovil reloj;

    @Autowired
    private ColaDeExtracciones cola;
    @Autowired
    private ModeloDePrueba modelo;

    private PersonasDeHistoria personas;
    private Persona personaA;
    private Persona personaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        USUARIOS.forEach(personas::borrar);
        modelo.reiniciar();
        reloj.poner(AHORA);
        personaA = personas.crear(USUARIO_A);
        personaB = personas.crear(USUARIO_B);
        tokenA = personas.token(personaA.cuentaId(), "PERSONA_MAYOR");
        tokenB = personas.token(personaB.cuentaId(), "PERSONA_MAYOR");
        tokenAdmin = personas.token(999_999L, "ADMINISTRADOR");
    }

    @AfterEach
    void limpiar() {
        esperarCola();
        USUARIOS.forEach(personas::borrar);
    }

    /** Responder encola extracciones: hay que esperar a que terminen antes de limpiar el grafo. */
    private void esperarCola() {
        assertThat(cola.esperarQueSeVacie(java.time.Duration.ofSeconds(30))).as("la cola se vació").isTrue();
    }

    // ---------- Acceso (sección 8) ----------

    @Test
    void sinTokenDevuelve401() throws Exception {
        for (MockHttpServletRequestBuilder pedido : losCuatroPedidos()) {
            mockMvc.perform(pedido)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
        }
    }

    @Test
    void administradorDevuelve403() throws Exception {
        for (MockHttpServletRequestBuilder pedido : losCuatroPedidos()) {
            mockMvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        }
        assertThat(nodosDePersona(personaA)).isZero();
    }

    @Test
    void unaCuentaSinPersonaMayorDevuelve404EnLosCuatro() throws Exception {
        String token = personas.token(personas.crearCuentaSinPersona(SIN_PERSONA), "PERSONA_MAYOR");
        for (MockHttpServletRequestBuilder pedido : losCuatroPedidos()) {
            mockMvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
        }
    }

    // ---------- GET /guion/proxima (7.1 y 5.2) ----------

    @Test
    void laPrimeraVezTraeLaPrimeraPreguntaConLaIntroduccionDeBienvenida() throws Exception {
        mockMvc.perform(pedir(get(PROXIMA), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pregunta.id").value(1))
                .andExpect(jsonPath("$.pregunta.orden").value(1))
                .andExpect(jsonPath("$.pregunta.texto").isNotEmpty())
                .andExpect(jsonPath("$.introduccion").value(PRIMERA_VEZ))
                .andExpect(jsonPath("$.mensajeFinal").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.quedanPendientes").value(true));
    }

    @Test
    void elGetCreaElNodoDeLaPersonaYRepetirloNoLoDuplica() throws Exception {
        assertThat(nodosDePersona(personaA)).isZero();

        mockMvc.perform(pedir(get(PROXIMA), tokenA)).andExpect(status().isOk());
        mockMvc.perform(pedir(get(PROXIMA), tokenA)).andExpect(status().isOk());
        mockMvc.perform(pedir(get(ESTADO), tokenA)).andExpect(status().isOk());

        assertThat(nodosDePersona(personaA)).isEqualTo(1);
    }

    @Test
    void lasTresFrasesNormalesSalenSegunElOrdenDeLaPregunta() throws Exception {
        responder(tokenA, 1, "Nací en un pueblo.");
        proximaEs(tokenA, 2, VAMOS_CON_LA_SIGUIENTE);   // orden 2: resto 2
        responder(tokenA, 2, "Una casa chica.");
        proximaEs(tokenA, 3, SIGAMOS);                  // orden 3: resto 0
        responder(tokenA, 3, "Con mis padres.");
        proximaEs(tokenA, 4, AHORA_ME_GUSTARIA);        // orden 4: resto 1
    }

    @Test
    void despuesDeUnSalteoLaIntroduccionEsLaDelSalteo() throws Exception {
        saltear(tokenA, 1);

        proximaEs(tokenA, 2, DESPUES_DE_SALTEO);
    }

    @Test
    void despuesDeUnSalteoYUnaRespuestaLaIntroduccionVuelveAserNormal() throws Exception {
        saltear(tokenA, 1);
        reloj.poner(AHORA.plusMinutes(1));
        responder(tokenA, 2, "Una casa chica.");

        proximaEs(tokenA, 3, SIGAMOS);
    }

    @Test
    void sePuedeResponderCualquierPreguntaPendienteYLaProximaSigueSiendoLaDeMenorOrden() throws Exception {
        responder(tokenA, 5, "A las escondidas.");

        // La última registrada fue una respuesta, así que la introducción es una normal (orden 1: resto 1).
        proximaEs(tokenA, 1, AHORA_ME_GUSTARIA);
    }

    @Test
    void conTodasRespondidasOSalteadasNoHayPreguntaYVieneElMensajeFinal() throws Exception {
        List<Integer> ids = idsDelGuion();
        for (int i = 0; i < ids.size(); i++) {
            if (i % 2 == 0) {
                responder(tokenA, ids.get(i), "Una respuesta.");
            } else {
                saltear(tokenA, ids.get(i));
            }
            reloj.poner(AHORA.plusMinutes(i + 1));
        }

        mockMvc.perform(pedir(get(PROXIMA), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pregunta").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.introduccion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.mensajeFinal").value(FINAL))
                .andExpect(jsonPath("$.quedanPendientes").value(false));
    }

    // ---------- GET /guion/estado (7.2) ----------

    @Test
    void sinNadaRegistradoTodoEstaPendienteYUltimaEsNull() throws Exception {
        int total = idsDelGuion().size();

        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidas").value(0))
                .andExpect(jsonPath("$.salteadas").value(0))
                .andExpect(jsonPath("$.pendientes").value(total))
                .andExpect(jsonPath("$.preguntas.length()").value(total))
                .andExpect(jsonPath("$.preguntas[0].id").value(1))
                .andExpect(jsonPath("$.preguntas[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.ultima").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void elEstadoCuentaRespondidasSalteadasYPendientesYTraeLaUltima() throws Exception {
        int total = idsDelGuion().size();
        responder(tokenA, 1, "Una respuesta.");
        reloj.poner(AHORA.plusMinutes(1));
        responder(tokenA, 2, "Otra respuesta.");
        reloj.poner(AHORA.plusMinutes(2));
        saltear(tokenA, 8);

        ResultActions respuesta = mockMvc.perform(pedir(get(ESTADO), tokenA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.respondidas").value(2))
                .andExpect(jsonPath("$.salteadas").value(1))
                .andExpect(jsonPath("$.pendientes").value(total - 3))
                .andExpect(jsonPath("$.preguntas[0].estado").value("RESPONDIDA"))
                .andExpect(jsonPath("$.preguntas[1].estado").value("RESPONDIDA"))
                .andExpect(jsonPath("$.preguntas[2].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.preguntas[7].id").value(8))
                .andExpect(jsonPath("$.preguntas[7].estado").value("SALTEADA"))
                .andExpect(jsonPath("$.ultima.preguntaId").value(8))
                .andExpect(jsonPath("$.ultima.estado").value("SALTEADA"));
        String fecha = com.jayway.jsonpath.JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(),
                "$.ultima.fecha");
        assertThat(fecha).startsWith("2026-11-04T10:02:00").endsWith("-03:00");
    }

    @Test
    void laUltimaEsLaMasRecienteNoLaDeMayorOrden() throws Exception {
        saltear(tokenA, 8);
        reloj.poner(AHORA.plusMinutes(1));
        responder(tokenA, 3, "Con mis padres.");

        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.ultima.preguntaId").value(3))
                .andExpect(jsonPath("$.ultima.estado").value("RESPONDIDA"));
    }

    @Test
    void conLaMismaFechaLaUltimaDesempataPorElMayorOrden() throws Exception {
        responder(tokenA, 5, "A las escondidas.");
        responder(tokenA, 3, "Con mis padres.");   // el reloj no se movió: misma fecha

        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.ultima.preguntaId").value(5));
    }

    // ---------- POST respuesta y salteo (7.3, 7.4 y 5.1 a 5.3) ----------

    @Test
    void responderDevuelve202ConEstadoACEPTADASinRespuestaYSaltearDevuelve204() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Nací en un pueblo.")))
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"estado\":\"ACEPTADA\"}", true));
        mockMvc.perform(pedir(post("/api/historia/guion/2/salteo"), tokenA))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(relaciones(personaA, 1)).isEqualTo(1);
        assertThat(relaciones(personaA, 2)).isEqualTo(1);
    }

    // ---------- Respuestas sin contenido: "no me acuerdo" es un salteo (contrato del grafo, 5.1) ----------

    private long relacionesDeTipo(Persona persona, int preguntaId, String tipo) {
        return contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:" + tipo
                + "]->(:PreguntaGuion {idPreguntaGuion: $q}) RETURN count(r) AS c", "p", persona.personaId(), "q",
                preguntaId);
    }

    @Test
    void noMeAcuerdoDevuelve202ACEPTADAPeroQuedaSalteadaSinRecuerdoNiPedidoAlModelo() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/10/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No me acuerdo.")))
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"estado\":\"ACEPTADA\"}", true));
        esperarCola();

        assertThat(relacionesDeTipo(personaA, 10, "SALTEO")).isEqualTo(1);
        assertThat(relacionesDeTipo(personaA, 10, "RESPONDIO")).isZero();
        assertThat(recuerdosDe(personaA)).as("no se creó ningún recuerdo").isZero();
        assertThat(modelo.pedidos()).as("no se le pidió nada al modelo").isEmpty();
        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.respondidas").value(0))
                .andExpect(jsonPath("$.salteadas").value(1))
                .andExpect(jsonPath("$.preguntas[9].estado").value("SALTEADA"));
    }

    @Test
    void laPreguntaNoSeVuelveAOfrecerYLaSiguienteLlevaLaIntroduccionDeDespuesDeUnSalteo() throws Exception {
        responder(tokenA, 1, "No me acuerdo");
        esperarCola();

        mockMvc.perform(pedir(get(PROXIMA), tokenA))
                .andExpect(jsonPath("$.pregunta.id").value(2))
                .andExpect(jsonPath("$.introduccion").value(DESPUES_DE_SALTEO));
        // Una vez salteada, responderla de nuevo da 409, igual que un salteo.
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Nací en un pueblo.")))
                .andExpect(status().isConflict());
    }

    @Test
    void variasFormasDeNoRecordarNoCreanRecuerdo() throws Exception {
        String[] respuestas = { "No sé", "Ni idea", "La verdad, no me acuerdo de nada", "No quiero contar",
                "No quiero responder", "Prefiero no hablar de eso" };
        for (int i = 0; i < respuestas.length; i++) {
            responder(tokenA, i + 1, respuestas[i]);
        }
        esperarCola();

        assertThat(recuerdosDe(personaA)).isZero();
        assertThat(modelo.pedidos()).isEmpty();
        for (int i = 1; i <= respuestas.length; i++) {
            assertThat(relacionesDeTipo(personaA, i, "SALTEO")).as("pregunta " + i).isEqualTo(1);
        }
    }

    @Test
    void unaRespuestaQueMezclaNoMeAcuerdoConContenidoSiCreaElRecuerdo() throws Exception {
        modelo.responde("""
                {"titulo":"Los tiempos con Marta","descripcion":"Pasaba el tiempo con Marta.","anio":null,\
                "anioAproximado":false,"edadMencionada":null,"personas":[{"nombre":"Marta","vinculo":null}],\
                "lugares":[]}""");

        responder(tokenA, 10, "No me acuerdo del año, pero iba con Marta.");
        esperarCola();

        assertThat(relacionesDeTipo(personaA, 10, "RESPONDIO")).isEqualTo(1);
        assertThat(relacionesDeTipo(personaA, 10, "SALTEO")).isZero();
        assertThat(recuerdosDe(personaA)).isEqualTo(1);
        assertThat(modelo.pedidos()).hasSize(1);
        assertThat(modelo.pedidos().get(0).mensaje()).contains("No me acuerdo del año, pero iba con Marta.");
    }

    @Test
    void conRiesgoGanaLaAlarmaAunqueTambienDigaQueNoSeAcuerda() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/10/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No me acuerdo, ya no quiero vivir")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
        esperarCola();

        assertThat(relaciones(personaA, 10)).as("la pregunta sigue pendiente").isZero();
        assertThat(recuerdosDe(personaA)).isZero();
    }

    @Test
    void noMeAcuerdoEnUnaPreguntaInexistenteDa404YEnUnaYaRegistradaDa409() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/9999/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No me acuerdo")))
                .andExpect(status().isNotFound());
        responder(tokenA, 1, "Nací en un pueblo.");
        esperarCola();

        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("No me acuerdo")))
                .andExpect(status().isConflict());
        assertThat(relacionesDeTipo(personaA, 1, "RESPONDIO")).isEqualTo(1);
        assertThat(relacionesDeTipo(personaA, 1, "SALTEO")).isZero();
    }

    @Test
    void elLogDeUnaRespuestaSinContenidoNoTraeElTexto(CapturedOutput salida) throws Exception {
        responder(tokenA, 1, "La verdad, no me acuerdo de nada");
        esperarCola();

        assertThat(salida.getAll()).contains("Respuesta sin contenido").doesNotContain("no me acuerdo de nada")
                .doesNotContain("La verdad");
    }

    // ---------- Alarmas en las respuestas del guion (contrato de historia de vida, 2.9, 5.6 y 7.1) ----------

    private static final String URGENCIA = "Lo que me contás puede ser urgente. Por favor, llamá ahora al 911 o "
            + "pedile a alguien que esté cerca que te ayude. Yo no puedo evaluar lo que te pasa, pero ellos sí.";
    private static final String MALESTAR = "Gracias por contármelo. Lo que sentís es importante y no tenés que "
            + "atravesarlo sin compañía. Podés llamar gratis a la Línea de Salud Mental 0800-999-0091, a "
            + "cualquier hora, o al Centro de Asistencia al Suicida 0800-345-1435. Si estás en peligro, llamá al "
            + "911. Si podés, contáselo también a alguien de tu confianza que esté cerca.";

    @Test
    void unaRespuestaConMalestarGraveDevuelve200ALARMAConElTextoDelChatYNoGuardaNiEncolaNada() throws Exception {
        alarma("Ya no quiero vivir", MALESTAR);
    }

    @Test
    void unaRespuestaConUrgenciaMedicaDevuelve200ALARMAConElTextoDelChatYNoGuardaNiEncolaNada() throws Exception {
        alarma("Me duele el pecho", URGENCIA);
    }

    private void alarma(String texto, String esperado) throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(texto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"))
                .andExpect(jsonPath("$.respuesta").value(esperado));
        esperarCola();

        assertThat(modelo.pedidos()).as("no se encoló ninguna extracción").isEmpty();
        assertThat(recuerdosDe(personaA)).as("no se guardó ningún recuerdo").isZero();
        assertThat(relaciones(personaA, 1)).as("la pregunta no quedó respondida").isZero();
        mockMvc.perform(pedir(get(PROXIMA), tokenA))
                .andExpect(jsonPath("$.pregunta.id").value(1))
                .andExpect(jsonPath("$.introduccion").value(PRIMERA_VEZ));
        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.respondidas").value(0))
                .andExpect(jsonPath("$.salteadas").value(0));
    }

    @Test
    void unaAlarmaNoDejaElTextoDeLaPersonaEnElLog(CapturedOutput salida) throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ya no quiero vivir, me lo contó Zulema Quiñones")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
        esperarCola();

        assertThat(salida.getAll()).doesNotContain("Zulema").doesNotContain("Quiñones")
                .doesNotContain("Ya no quiero vivir");
    }

    @Test
    void despuesDeUnaAlarmaLaMismaPreguntaSePuedeResponderNormalmente() throws Exception {
        alarma("Ya no quiero vivir", MALESTAR);

        responder(tokenA, 1, "Nací en un pueblo.");
        esperarCola();

        assertThat(relaciones(personaA, 1)).isEqualTo(1);
        assertThat(recuerdosDe(personaA)).isEqualTo(1);
    }

    @Test
    void conRiesgoNoSeValidaQueLaPreguntaExistaNiQueSigaPendiente() throws Exception {
        responder(tokenA, 1, "Una respuesta.");
        esperarCola();

        // Ya respondida: sin riesgo daría 409; con riesgo, 200 ALARMA (la seguridad va primero).
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ya no quiero vivir")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
        // Inexistente: sin riesgo daría 404.
        mockMvc.perform(pedir(post("/api/historia/guion/9999/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Me duele el pecho")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
        assertThat(relaciones(personaA, 1)).isEqualTo(1);
    }

    @Test
    void unaRespuestaDeRiesgoMuyLargaDa400AntesDeDetectar() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ya no quiero vivir " + "a".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].mensaje").value(MUY_LARGA));
    }

    /**
     * LÍMITE CONOCIDO (a anotar en el contrato): una frase de duelo con "me quería morir de la tristeza" NO activa
     * la alarma, porque las listas de {@code chat.properties} detectan "me quiero morir", "quiero morirme" y
     * "quisiera/quería morirme", pero no "me quería morir". Es el mismo límite del chat (contrato de chat, 5.3 y
     * T8): una formulación indirecta o en pasado no se detecta. Esta prueba solo documenta el resultado de hoy;
     * si se ajusta el detector, hay que actualizarla.
     */
    @Test
    void limiteConocido_unaFraseDeDueloConMeQueriaMorirNoActivaLaAlarma() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("cuando se murió mi mamá me quería morir de la tristeza")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
        esperarCola();

        assertThat(relaciones(personaA, 1)).isEqualTo(1);
    }

    @Test
    void unaPreguntaYaRespondidaOSalteadaDa409ConElFormatoDelContrato() throws Exception {
        responder(tokenA, 1, "Una respuesta.");
        saltear(tokenA, 2);

        // Responder dos veces, saltear una respondida, responder una salteada y saltear dos veces.
        for (ResultActions r : List.of(
                mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Otra vez."))),
                mockMvc.perform(pedir(post("/api/historia/guion/1/salteo"), tokenA)),
                mockMvc.perform(pedir(post("/api/historia/guion/2/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Ahora sí."))),
                mockMvc.perform(pedir(post("/api/historia/guion/2/salteo"), tokenA)))) {
            r.andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflicto"))
                    .andExpect(jsonPath("$.detail").value(YA_RESPONDIDA))
                    .andExpect(jsonPath("$.instance").value(org.hamcrest.Matchers.startsWith("/api/historia/guion/")));
        }

        // Cada pregunta quedó con una sola relación, y el estado no cambió.
        assertThat(relaciones(personaA, 1)).isEqualTo(1);
        assertThat(relaciones(personaA, 2)).isEqualTo(1);
        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.respondidas").value(1))
                .andExpect(jsonPath("$.salteadas").value(1));
    }

    @Test
    void laRespuestaVaciaOSinTextoDa400ConElMensajeDelContrato() throws Exception {
        for (String cuerpo : List.of("{\"texto\":\"\"}", "{\"texto\":\"   \"}", "{}", "{\"texto\":null}")) {
            mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS))
                    .andExpect(jsonPath("$.errores[0].campo").value("texto"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value(OBLIGATORIA));
        }
        assertThat(relaciones(personaA, 1)).isZero();
    }

    @Test
    void laRespuestaDeMasDeMilCaracteresDa400YLaDeMilSeAcepta() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("a".repeat(1001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS))
                .andExpect(jsonPath("$.errores[0].campo").value("texto"))
                .andExpect(jsonPath("$.errores[0].mensaje").value(MUY_LARGA));
        assertThat(relaciones(personaA, 1)).isZero();

        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("a".repeat(1000))))
                .andExpect(status().isAccepted());
        assertThat(relaciones(personaA, 1)).isEqualTo(1);
    }

    @Test
    void unPedidoInvalidoNoCreaNadaEnElGrafo() throws Exception {
        assertThat(nodosDePersona(personaA)).isZero();

        // Cuerpo vacío, respuesta demasiado larga y pregunta inexistente con cuerpo inválido: 400 en los tres,
        // y la validación va antes de tocar el grafo (5.1, paso 2).
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(pedir(post("/api/historia/guion/1/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("a".repeat(1001)))).andExpect(status().isBadRequest());
        mockMvc.perform(pedir(post("/api/historia/guion/999/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"\"}")).andExpect(status().isBadRequest());

        assertThat(nodosDePersona(personaA)).isZero();
    }

    @Test
    void unaPreguntaInexistenteDa404EnRespuestaYSalteo() throws Exception {
        for (String id : List.of("999", "0", "-1")) {
            mockMvc.perform(pedir(post("/api/historia/guion/" + id + "/respuesta"), tokenA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("Una respuesta.")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
            mockMvc.perform(pedir(post("/api/historia/guion/" + id + "/salteo"), tokenA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
        }
    }

    @Test
    void unIdDePreguntaQueNoEsUnNumeroDa400() throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/abc/salteo"), tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS));
    }

    // ---------- Aislamiento entre personas ----------

    @Test
    void elProgresoDeUnaPersonaNoCambiaElDeOtra() throws Exception {
        responder(tokenA, 1, "Una respuesta.");
        responder(tokenA, 2, "Otra respuesta.");
        saltear(tokenA, 8);

        // B no ve nada de lo de A.
        proximaEs(tokenB, 1, PRIMERA_VEZ);
        mockMvc.perform(pedir(get(ESTADO), tokenB))
                .andExpect(jsonPath("$.respondidas").value(0))
                .andExpect(jsonPath("$.salteadas").value(0))
                .andExpect(jsonPath("$.ultima").value(org.hamcrest.Matchers.nullValue()));

        // B responde la 1: A sigue igual, y las dos pueden responder la misma pregunta sin conflicto.
        responder(tokenB, 1, "La mía.");
        mockMvc.perform(pedir(get(ESTADO), tokenA))
                .andExpect(jsonPath("$.respondidas").value(2))
                .andExpect(jsonPath("$.salteadas").value(1));
        mockMvc.perform(pedir(get(ESTADO), tokenB))
                .andExpect(jsonPath("$.respondidas").value(1))
                .andExpect(jsonPath("$.salteadas").value(0));
        assertThat(relaciones(personaA, 1)).isEqualTo(1);
        assertThat(relaciones(personaB, 1)).isEqualTo(1);
    }

    @Test
    void laPersonaSaleDelTokenYNoDeLaUrl() throws Exception {
        // No hay forma de elegir otra persona: ninguna ruta lleva un id de persona.
        responder(tokenA, 1, "Una respuesta.");

        assertThat(relacionesDePersona(personaA)).isEqualTo(1);
        assertThat(relacionesDePersona(personaB)).isZero();
    }

    // ---------- Piezas ----------

    private List<MockHttpServletRequestBuilder> losCuatroPedidos() {
        return List.of(get(PROXIMA), get(ESTADO),
                post("/api/historia/guion/1/respuesta").contentType(MediaType.APPLICATION_JSON).content(cuerpo("Hola.")),
                post("/api/historia/guion/1/salteo"));
    }

    private static MockHttpServletRequestBuilder pedir(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static String cuerpo(String texto) {
        return "{\"texto\":\"" + texto + "\"}";
    }

    private void responder(String token, int preguntaId, String texto) throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/" + preguntaId + "/respuesta"), token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo(texto)))
                .andExpect(status().isAccepted());
    }

    private void saltear(String token, int preguntaId) throws Exception {
        mockMvc.perform(pedir(post("/api/historia/guion/" + preguntaId + "/salteo"), token))
                .andExpect(status().isNoContent());
    }

    private void proximaEs(String token, int preguntaId, String introduccion) throws Exception {
        mockMvc.perform(pedir(get(PROXIMA), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pregunta.id").value(preguntaId))
                .andExpect(jsonPath("$.introduccion").value(introduccion))
                .andExpect(jsonPath("$.quedanPendientes").value(true));
    }

    /** Los ids del guion, en orden, tal como los ve la persona. */
    private List<Integer> idsDelGuion() throws Exception {
        String json = mockMvc.perform(pedir(get(ESTADO), tokenA)).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> preguntas = com.jayway.jsonpath.JsonPath.read(json, "$.preguntas");
        return preguntas.stream().map(p -> ((Number) p.get("id")).intValue()).toList();
    }

    private long nodosDePersona(Persona persona) {
        return contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p", persona.personaId());
    }

    private long relaciones(Persona persona, int preguntaId) {
        return contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->(:PreguntaGuion {idPreguntaGuion: $q}) "
                + "RETURN count(r) AS c", "p", persona.personaId(), "q", preguntaId);
    }

    private long recuerdosDe(Persona persona) {
        return contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r:Recuerdo) "
                + "RETURN count(r) AS c", "p", persona.personaId());
    }

    private long relacionesDePersona(Persona persona) {
        return contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->() RETURN count(r) AS c",
                "p", persona.personaId());
    }
}
