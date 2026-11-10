package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los endpoints de repreguntas (contrato del grafo, 7.5 a 7.7, secciones 6, 8 y 9), de punta a punta con
 * {@code MockMvc} contra el Postgres y el Neo4j de {@code docker compose up -d} y el modelo de prueba: acceso
 * (401, 403, 404), códigos HTTP, validaciones (400), conflictos (409) y los esquemas de {@code /v3/api-docs}.
 * Las reglas de negocio finas están en {@link RepreguntaServiceIntegrationTests}.
 *
 * <p>Preguntas del guion: la 7 repregunta por {@code [ANIO, LUGARES, PERSONAS]} y la 1 por {@code []}.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Import(ModeloDePruebaConfig.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RepreguntaEndpointsIntegrationTests {

    private static final String SIGUIENTE = "/api/historia/repreguntas/siguiente";
    private static final String USUARIO_A = "test_historia_rep_ep_a";
    private static final String USUARIO_B = "test_historia_rep_ep_b";
    private static final String SIN_PERSONA = "test_historia_rep_ep_sin_persona";
    private static final List<String> USUARIOS = List.of(USUARIO_A, USUARIO_B, SIN_PERSONA);
    private static final int ESCUELA = 7;
    private static final int SIN_REPREGUNTAS = 1;

    private static final String DATOS_INVALIDOS = "Revisá los datos ingresados.";
    private static final String NO_ENCONTRADO = "No encontramos lo que buscás.";
    private static final String YA_NO_HACE_FALTA = "Esa pregunta ya no hace falta.";
    private static final String OBLIGATORIA = "Escribí una respuesta o elegí saltear la pregunta.";
    private static final String MUY_LARGA = "Tu respuesta es muy larga. Probá con una más corta.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private ColaDeExtracciones cola;
    @Autowired
    private ModeloDePrueba modelo;
    @Autowired
    private Neo4jClient neo4j;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JsonMapper jsonMapper;

    private PersonasDeHistoria personas;
    private RecuerdosDePrueba recuerdos;
    private Persona personaA;
    private Persona personaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;
    private int minutos;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        recuerdos = new RecuerdosDePrueba(neo4j);
        esperarCola();
        modelo.reiniciar();
        USUARIOS.forEach(personas::borrar);
        personaA = personas.crear(USUARIO_A);
        personaB = personas.crear(USUARIO_B);
        tokenA = personas.token(personaA.cuentaId(), "PERSONA_MAYOR");
        tokenB = personas.token(personaB.cuentaId(), "PERSONA_MAYOR");
        tokenAdmin = personas.token(999_999L, "ADMINISTRADOR");
        minutos = 0;
    }

    @AfterEach
    void limpiar() {
        esperarCola();
        USUARIOS.forEach(personas::borrar);
        modelo.reiniciar();
    }

    private void esperarCola() {
        assertThat(cola.esperarQueSeVacie(Duration.ofSeconds(20))).as("la cola se vació").isTrue();
    }

    private String recuerdoSinAnio(Persona quien, int pregunta) {
        return recuerdos.crear(quien.personaId(), pregunta, "Un recuerdo", null, null, 0, 0, 0, minutos++);
    }

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static MockHttpServletRequestBuilder responder(String recuerdoId, String dato, String json) {
        return post("/api/historia/repreguntas/" + recuerdoId + "/" + dato + "/respuesta")
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder saltear(String recuerdoId, String dato) {
        return post("/api/historia/repreguntas/" + recuerdoId + "/" + dato + "/salteo");
    }

    private static String cuerpo(String texto) {
        return "{\"texto\":\"" + texto + "\"}";
    }

    // ---------- Acceso (sección 8) ----------

    private List<MockHttpServletRequestBuilder> lasTresRutas() {
        String id = UUID.randomUUID().toString();
        return List.of(get(SIGUIENTE), responder(id, "ANIO", cuerpo("Fue en 1962")), saltear(id, "ANIO"));
    }

    @Test
    void sinTokenDevuelve401EnLasTres() throws Exception {
        for (MockHttpServletRequestBuilder pedido : lasTresRutas()) {
            mockMvc.perform(pedido)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
        }
    }

    @Test
    void administradorDevuelve403EnLasTres() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);
        for (MockHttpServletRequestBuilder pedido : List.of(get(SIGUIENTE), responder(id, "ANIO", cuerpo("Fue en 1962")),
                saltear(id, "ANIO"))) {
            mockMvc.perform(con(pedido, tokenAdmin))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        }
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L);
    }

    @Test
    void unaCuentaSinPersonaMayorDevuelve404EnLasTres() throws Exception {
        String token = personas.token(personas.crearCuentaSinPersona(SIN_PERSONA), "PERSONA_MAYOR");
        for (MockHttpServletRequestBuilder pedido : lasTresRutas()) {
            mockMvc.perform(con(pedido, token))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
        }
    }

    // ---------- 7.5: GET /repreguntas/siguiente ----------

    @Test
    void sinRecuerdosDevuelve200ConNingunaYRepreguntaNula() throws Exception {
        mockMvc.perform(con(get(SIGUIENTE), tokenA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.estado").value("NINGUNA"))
                .andExpect(jsonPath("$.repregunta").value(nullValue()));
    }

    @Test
    void conUnaCandidataDevuelveHayConElRecuerdoElDatoYElTexto() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(get(SIGUIENTE), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("HAY"))
                .andExpect(jsonPath("$.repregunta.recuerdoId").value(id))
                .andExpect(jsonPath("$.repregunta.dato").value("ANIO"))
                .andExpect(jsonPath("$.repregunta.texto").value("¿Te acordás en qué año fue, o cuántos años tenías?"));
    }

    @Test
    void excluirSeparadoPorComaSacaEsosDatos() throws Exception {
        recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(get(SIGUIENTE).param("excluir", "ANIO"), tokenA))
                .andExpect(jsonPath("$.repregunta.dato").value("LUGARES"))
                .andExpect(jsonPath("$.repregunta.texto").value("¿Dónde pasó esto?"));
        mockMvc.perform(con(get(SIGUIENTE).param("excluir", "ANIO,LUGARES"), tokenA))
                .andExpect(jsonPath("$.repregunta.dato").value("PERSONAS"))
                .andExpect(jsonPath("$.repregunta.texto").value("¿Quién más estaba con vos en ese momento?"));
        mockMvc.perform(con(get(SIGUIENTE).param("excluir", "ANIO,LUGARES,PERSONAS"), tokenA))
                .andExpect(jsonPath("$.estado").value("NINGUNA"));
        mockMvc.perform(con(get(SIGUIENTE).param("excluir", ""), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repregunta.dato").value("ANIO"));
    }

    @Test
    void unValorInvalidoEnExcluirDa400ConElTextoGeneral() throws Exception {
        recuerdoSinAnio(personaA, ESCUELA);

        for (String invalido : List.of("FECHA", "ANIO,FECHA", "anio")) {
            mockMvc.perform(con(get(SIGUIENTE).param("excluir", invalido), tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS));
        }
    }

    @Test
    void unPreguntaIdQueNoEsUnNumeroDa400() throws Exception {
        mockMvc.perform(con(get(SIGUIENTE).param("preguntaId", "abc"), tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS));
    }

    @Test
    void conPreguntaIdBuscaEnEseRecuerdoYUnoDesconocidoDaNingunaConStatus200() throws Exception {
        recuerdoSinAnio(personaA, ESCUELA);
        String deLaCasa = recuerdoSinAnio(personaA, 2);

        mockMvc.perform(con(get(SIGUIENTE).param("preguntaId", "2"), tokenA))
                .andExpect(jsonPath("$.repregunta.recuerdoId").value(deLaCasa));
        mockMvc.perform(con(get(SIGUIENTE).param("preguntaId", "9999"), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NINGUNA"));
    }

    @Test
    void mientrasHayUnaExtraccionEnCursoDevuelvePROCESANDOSinRepregunta() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);
        modelo.respondeConDemora(ModeloDePrueba.JSON_GENERICO, 600);
        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Fue en 1962")), tokenA)).andExpect(status().isAccepted());

        mockMvc.perform(con(get(SIGUIENTE), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROCESANDO"))
                .andExpect(jsonPath("$.repregunta").value(nullValue()));
        // Es por persona: la otra persona no está procesando nada.
        mockMvc.perform(con(get(SIGUIENTE), tokenB)).andExpect(jsonPath("$.estado").value("NINGUNA"));
        esperarCola();
    }

    @Test
    void losRecuerdosDeOtraPersonaNoAparecen() throws Exception {
        recuerdoSinAnio(personaB, ESCUELA);

        mockMvc.perform(con(get(SIGUIENTE), tokenA)).andExpect(jsonPath("$.estado").value("NINGUNA"));
        mockMvc.perform(con(get(SIGUIENTE), tokenB)).andExpect(jsonPath("$.estado").value("HAY"));
    }

    // ---------- 7.6: POST /repreguntas/{recuerdoId}/{dato}/respuesta ----------

    @Test
    void responderDevuelve202ConEstadoACEPTADAYCompletaElRecuerdo() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);
        modelo.responde("{\"titulo\":\"X\",\"descripcion\":\"Y\",\"anio\":1962,\"anioAproximado\":false,"
                + "\"edadMencionada\":null,\"personas\":[],\"lugares\":[]}");

        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Fue en 1962")), tokenA))
                .andExpect(status().isAccepted())
                .andExpect(content().json("{\"estado\":\"ACEPTADA\"}", true));
        esperarCola();

        assertThat(recuerdos.propiedades(id)).containsEntry("anio", 1962L).containsEntry("precision", "EXACTA")
                .containsEntry("intentosAnio", 1L).containsEntry("titulo", "Un recuerdo");
        mockMvc.perform(con(get(SIGUIENTE), tokenA)).andExpect(jsonPath("$.repregunta.dato").value("LUGARES"));
    }

    // ---------- Alarmas (contrato de historia de vida, 2.9, 5.6 y 7.2) ----------

    @Test
    void unaRepreguntaConMalestarGraveDevuelve200ALARMAYNoCuentaIntentoNiCompletaElDato() throws Exception {
        alarmaEnRepregunta("Fue en 1962 pero ya no quiero vivir más",
                "Gracias por contármelo. Lo que sentís es importante y no tenés que atravesarlo sin compañía. "
                        + "Podés llamar gratis a la Línea de Salud Mental 0800-999-0091, a cualquier hora, o al "
                        + "Centro de Asistencia al Suicida 0800-345-1435. Si estás en peligro, llamá al 911. Si "
                        + "podés, contáselo también a alguien de tu confianza que esté cerca.");
    }

    @Test
    void unaRepreguntaConUrgenciaMedicaDevuelve200ALARMAYNoCuentaIntentoNiCompletaElDato() throws Exception {
        alarmaEnRepregunta("Me duele el pecho desde 1962",
                "Lo que me contás puede ser urgente. Por favor, llamá ahora al 911 o pedile a alguien que esté "
                        + "cerca que te ayude. Yo no puedo evaluar lo que te pasa, pero ellos sí.");
    }

    private void alarmaEnRepregunta(String texto, String esperado) throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(responder(id, "ANIO", cuerpo(texto)), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"))
                .andExpect(jsonPath("$.respuesta").value(esperado));
        esperarCola();

        assertThat(modelo.pedidos()).as("no se encoló ninguna extracción").isEmpty();
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L).containsEntry("anio", null)
                .containsEntry("titulo", "Un recuerdo");
        // El dato sigue faltando y se vuelve a ofrecer.
        mockMvc.perform(con(get(SIGUIENTE), tokenA))
                .andExpect(jsonPath("$.estado").value("HAY"))
                .andExpect(jsonPath("$.repregunta.recuerdoId").value(id))
                .andExpect(jsonPath("$.repregunta.dato").value("ANIO"));
        // Y se puede responder con normalidad: ahí sí cuenta el intento.
        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Fue en 1962")), tokenA)).andExpect(status().isAccepted());
        esperarCola();
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 1L);
    }

    @Test
    void unaAlarmaEnUnaRepreguntaNoDejaElTextoDeLaPersonaEnElLog(CapturedOutput salida) throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Me duele el pecho, lo vio Zulema Quiñones")), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
        esperarCola();

        assertThat(salida.getAll()).doesNotContain("Zulema").doesNotContain("Quiñones")
                .doesNotContain("Me duele el pecho");
    }

    @Test
    void unaRespuestaDeRiesgoMuyLargaDa400AntesDeDetectarYNoCuentaIntento() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        // Largo y con palabras de riesgo: el 400 por largo va primero (5.6), no la alarma ni el intento.
        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Ya no quiero vivir más " + "x".repeat(1001))), tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("texto"))
                .andExpect(jsonPath("$.errores[0].mensaje").value(MUY_LARGA));

        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void conRiesgoNoSeValidaQueElRecuerdoExista() throws Exception {
        // Sin riesgo daría 404; con riesgo, 200 ALARMA (la seguridad va primero, decisión del contrato 5.6).
        mockMvc.perform(con(responder(java.util.UUID.randomUUID().toString(), "ANIO", cuerpo("Me duele el pecho")),
                        tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ALARMA"));
    }

    @Test
    void unaRespuestaVaciaOSoloEspaciosDa400ConElMensajeDelContrato() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        for (String json : List.of("{\"texto\":\"\"}", "{\"texto\":\"   \"}", "{\"texto\":null}", "{}")) {
            mockMvc.perform(con(responder(id, "ANIO", json), tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.errores[0].campo").value("texto"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value(OBLIGATORIA));
        }
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void unaRespuestaDeMasDe1000CaracteresDa400ConElMensajeDelContrato() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(responder(id, "ANIO", cuerpo("x".repeat(1001))), tokenA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("texto"))
                .andExpect(jsonPath("$.errores[0].mensaje").value(MUY_LARGA));
        mockMvc.perform(con(responder(id, "ANIO", cuerpo("x".repeat(1000))), tokenA)).andExpect(status().isAccepted());
        esperarCola();
        assertThat(recuerdos.propiedades(id)).as("solo la de 1000 contó").containsEntry("intentosAnio", 1L);
    }

    @Test
    void unCuerpoAusenteODeOtroTipoDa400() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(post("/api/historia/repreguntas/" + id + "/ANIO/respuesta"), tokenA))
                .andExpect(status().isBadRequest());
        mockMvc.perform(con(responder(id, "ANIO", "no es json"), tokenA)).andExpect(status().isBadRequest());
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L);
    }

    @Test
    void unDatoInvalidoEnLaRutaDa400EnResponderYEnSaltear() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        for (String dato : List.of("FECHA", "anio", "LUGAR")) {
            mockMvc.perform(con(responder(id, dato, cuerpo("Fue en 1962")), tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS));
            mockMvc.perform(con(saltear(id, dato), tokenA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(DATOS_INVALIDOS));
        }
        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 0L);
    }

    @Test
    void unRecuerdoQueNoExisteODeOtraPersonaDa404EnResponderYEnSaltear() throws Exception {
        String deLaOtra = recuerdoSinAnio(personaB, ESCUELA);

        for (String id : List.of(UUID.randomUUID().toString(), deLaOtra, "no-es-un-uuid")) {
            mockMvc.perform(con(responder(id, "ANIO", cuerpo("Fue en 1962")), tokenA))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
            mockMvc.perform(con(saltear(id, "ANIO"), tokenA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(NO_ENCONTRADO));
        }
        assertThat(recuerdos.propiedades(deLaOtra)).containsEntry("intentosAnio", 0L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void conElMaximoDeIntentosDa409ConElTextoDelContratoYNoEncolaNada() throws Exception {
        String id = recuerdos.crear(personaA.personaId(), ESCUELA, "Un recuerdo", null, null, 2, 0, 0, minutos++);

        mockMvc.perform(con(responder(id, "ANIO", cuerpo("Fue en 1962")), tokenA))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andExpect(jsonPath("$.detail").value(YA_NO_HACE_FALTA));
        mockMvc.perform(con(saltear(id, "ANIO"), tokenA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(YA_NO_HACE_FALTA));
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void unDatoQueNoAplicaOYaNoFaltaDa409() throws Exception {
        String sinLista = recuerdoSinAnio(personaA, SIN_REPREGUNTAS);
        String conAnio = recuerdos.crear(personaA.personaId(), ESCUELA, "Con año", 1957, "EXACTA", 0, 0, 0, minutos++);
        String conLugar = recuerdoSinAnio(personaA, ESCUELA);
        recuerdos.agregarLugar(conLugar, "Allen");

        mockMvc.perform(con(responder(sinLista, "ANIO", cuerpo("Fue en 1962")), tokenA)).andExpect(status().isConflict());
        mockMvc.perform(con(responder(conAnio, "ANIO", cuerpo("Fue en 1957")), tokenA)).andExpect(status().isConflict());
        mockMvc.perform(con(saltear(conLugar, "LUGARES"), tokenA)).andExpect(status().isConflict());
        assertThat(recuerdos.propiedades(conLugar)).containsEntry("intentosLugares", 0L);
    }

    // ---------- 7.7: POST /repreguntas/{recuerdoId}/{dato}/salteo ----------

    @Test
    void saltearDevuelve204SinCuerpoYSumaUnIntentoSinEncolarNada() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(saltear(id, "ANIO"), tokenA))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(recuerdos.propiedades(id)).containsEntry("intentosAnio", 1L).containsEntry("anio", null);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void dosSaltosAgotanElDatoYElTercerPedidoDa409() throws Exception {
        String id = recuerdoSinAnio(personaA, ESCUELA);

        mockMvc.perform(con(saltear(id, "ANIO"), tokenA)).andExpect(status().isNoContent());
        mockMvc.perform(con(saltear(id, "ANIO"), tokenA)).andExpect(status().isNoContent());
        mockMvc.perform(con(saltear(id, "ANIO"), tokenA)).andExpect(status().isConflict());
        mockMvc.perform(con(get(SIGUIENTE), tokenA)).andExpect(jsonPath("$.repregunta.dato").value("LUGARES"));
    }

    // ---------- /v3/api-docs ----------

    private JsonNode apiDocs() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        return jsonMapper.readTree(json);
    }

    private static String referencia(JsonNode operacion) {
        return operacion.get("requestBody").get("content").get("application/json").get("schema").get("$ref").asString();
    }

    @Test
    void losTresEndpointsEstanEnLosEsquemasConLosTiposCorrectos() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode rutas = docs.get("paths");
        JsonNode esquemas = docs.get("components").get("schemas");

        // 7.5: GET con los dos parámetros opcionales.
        JsonNode siguiente = rutas.get("/api/historia/repreguntas/siguiente").get("get");
        assertThat(siguiente).isNotNull();
        List<String> parametros = siguiente.get("parameters").valueStream().map(p -> p.get("name").asString()).toList();
        assertThat(parametros).containsExactlyInAnyOrder("preguntaId", "excluir");
        siguiente.get("parameters").forEach(p -> assertThat(p.get("required") == null || !p.get("required").asBoolean())
                .as("opcional: " + p.get("name")).isTrue());
        String respuesta = siguiente.get("responses").get("200").get("content").get("*/*").get("schema").get("$ref")
                .asString();
        assertThat(respuesta).endsWith("/RepreguntaSiguienteResponse");
        assertThat(valores(esquemas.get("RepreguntaSiguienteResponse").get("properties").get("estado")))
                .containsExactlyInAnyOrder("PROCESANDO", "HAY", "NINGUNA");
        assertThat(valores(esquemas.get("RepreguntaDto").get("properties").get("dato")))
                .containsExactlyInAnyOrder("ANIO", "LUGARES", "PERSONAS");

        // 7.6: el cuerpo es el de texto de historia, no el de juegos.
        JsonNode responder = rutas.get("/api/historia/repreguntas/{recuerdoId}/{dato}/respuesta").get("post");
        assertThat(referencia(responder)).endsWith("/RespuestaTextoRequest");
        assertThat(responder.get("responses").has("202")).as("7.2 documenta 202 (ACEPTADA)").isTrue();
        // Desde el contrato de historia de vida (7.2) el 200 es intencional: es la ALARMA.
        assertThat(responder.get("responses").has("200")).as("y 200 (ALARMA)").isTrue();
        JsonNode dato = responder.get("parameters").valueStream().filter(p -> p.get("name").asString().equals("dato"))
                .findFirst().orElseThrow();
        assertThat(valores(dato.get("schema"))).containsExactlyInAnyOrder("ANIO", "LUGARES", "PERSONAS");
        assertThat(esquemas.get("RespuestaTextoRequest").get("properties").has("texto")).isTrue();
        assertThat(esquemas.get("RespuestaTextoRequest").get("properties").has("ejercicioId")).isFalse();

        // 7.7: sin cuerpo y con 204.
        JsonNode saltear = rutas.get("/api/historia/repreguntas/{recuerdoId}/{dato}/salteo").get("post");
        assertThat(saltear.has("requestBody")).isFalse();
        assertThat(saltear.get("responses").has("204")).as("7.7 documenta 204").isTrue();
        assertThat(saltear.get("responses").has("200")).as("y no un 200 por defecto").isFalse();
    }

    @Test
    void elEndpointDeJuegosSigueConSuPropioEsquemaYElDelGuionConElDeTexto() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode rutas = docs.get("paths");
        JsonNode esquemas = docs.get("components").get("schemas");

        assertThat(referencia(rutas.get("/api/juegos/respuesta").get("post"))).endsWith("/RespuestaRequest");
        assertThat(esquemas.get("RespuestaRequest").get("properties").has("ejercicioId")).isTrue();
        JsonNode guion = rutas.get("/api/historia/guion/{preguntaId}/respuesta").get("post");
        assertThat(referencia(guion)).endsWith("/RespuestaTextoRequest");
        assertThat(guion.get("responses").has("202")).as("7.3 documenta 202").isTrue();
        assertThat(guion.get("responses").has("200")).as("y 200 (ALARMA, contrato de historia de vida 7.1)").isTrue();
        assertThat(rutas.get("/api/historia/guion/{preguntaId}/salteo").get("post").get("responses").has("204"))
                .as("7.4 documenta 204").isTrue();
    }

    private static List<String> valores(JsonNode esquema) {
        return esquema.get("enum").valueStream().map(JsonNode::asString).toList();
    }
}
