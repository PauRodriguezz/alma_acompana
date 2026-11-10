package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

/**
 * {@code {progreso}} y {@code {recuerdos}} en el prompt del chat (contrato de historia de vida, 5.5 y 13), de punta
 * a punta: {@code POST /api/chat/mensajes} contra el Postgres y el Neo4j de {@code docker compose up -d}, leyendo el
 * prompt que recibe el modelo de prueba. Ejercita P13 (persona), P14 (lugar), P15 (etapa), P16 y P17. Los recuerdos
 * se arman directo en el grafo (pregunta 1 del guion real).
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(ModeloDePruebaConfig.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RecuerdosEnElChatIntegrationTests {

    private static final String USUARIO_A = "test_historia_chat_a";
    private static final String USUARIO_B = "test_historia_chat_b";
    private static final List<String> USUARIOS = List.of(USUARIO_A, USUARIO_B);
    private static final String SIN_INFORMACION = "No hay información.";
    private static final Pattern PASO_DE_ORDEN = Pattern.compile("^- ", Pattern.MULTILINE);

    private static final String LINEA_ELENA = "- Mi maestra Elena (1957): Su maestra de primer grado, que le enseñó a "
            + "leer. Con Elena (maestra), en Escuela N° 12.";
    private static final String LINEA_SIESTAS = "- Las siestas en el patio (1955): Dormía la siesta bajo la parra. "
            + "Con Rosa (abuela), en Casa de la abuela.";
    private static final String LINEA_CARLOS = "- Conocí a Carlos (1975): Lo conoció en un baile. Con Carlos (marido).";

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

    private PersonasDeHistoria personas;
    private RecuerdosDePrueba recuerdos;
    private Persona personaA;
    private Persona personaB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        recuerdos = new RecuerdosDePrueba(neo4j);
        esperarCola();
        USUARIOS.forEach(personas::borrar);
        modelo.reiniciar();
        personaA = personas.crear(USUARIO_A);
        personaB = personas.crear(USUARIO_B);
        tokenA = personas.token(personaA.cuentaId(), "PERSONA_MAYOR");
        tokenB = personas.token(personaB.cuentaId(), "PERSONA_MAYOR");
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

    // ---------- Armado de datos ----------

    /** Elena (maestra), Escuela N° 12, 1957, INFANCIA. */
    private String elena(Persona quien, int minutos) {
        String id = recuerdos.crear(quien.personaId(), 1, "Mi maestra Elena", 1957, "EXACTA", 0, 0, 0, minutos);
        recuerdos.descripcion(id, "Su maestra de primer grado, que le enseñó a leer.");
        recuerdos.agregarPersona(quien.personaId(), id, "Elena", "maestra");
        recuerdos.agregarLugar(id, "Escuela N° 12");
        return id;
    }

    /** Rosa (abuela), Casa de la abuela, 1955, INFANCIA. */
    private String siestas(Persona quien, int minutos) {
        String id = recuerdos.crear(quien.personaId(), 1, "Las siestas en el patio", 1955, "EXACTA", 0, 0, 0, minutos);
        recuerdos.descripcion(id, "Dormía la siesta bajo la parra.");
        recuerdos.agregarPersona(quien.personaId(), id, "Rosa", "abuela");
        recuerdos.agregarLugar(id, "Casa de la abuela");
        return id;
    }

    /** Carlos (marido), 1975, JUVENTUD, sin lugar. */
    private String carlos(Persona quien, int minutos) {
        String id = recuerdos.crear(quien.personaId(), 1, "Conocí a Carlos", 1975, "EXACTA", 0, 0, 0, minutos);
        recuerdos.descripcion(id, "Lo conoció en un baile.");
        recuerdos.etapa(id, "JUVENTUD");
        recuerdos.agregarPersona(quien.personaId(), id, "Carlos", "marido");
        return id;
    }

    // ---------- El chat y el prompt ----------

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** Manda un mensaje al chat y devuelve el prompt (instrucciones) que recibió el modelo. */
    private String prompt(String token, String mensaje) throws Exception {
        int antes = modelo.pedidos().size();
        mockMvc.perform(con(post("/api/chat/mensajes"), token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"" + mensaje + "\"}"))
                .andExpect(status().isOk());
        assertThat(modelo.pedidos()).as("el modelo se llamó una vez").hasSize(antes + 1);
        return modelo.pedidos().get(antes).instrucciones();
    }

    private static String recuerdosDelPrompt(String prompt) {
        int inicio = prompt.indexOf("Recuerdos relacionados:");
        assertThat(inicio).as("el prompt trae la sección de recuerdos").isGreaterThanOrEqualTo(0);
        return prompt.substring(inicio + "Recuerdos relacionados:".length()).strip();
    }

    private static String progresoDelPrompt(String prompt) {
        int inicio = prompt.indexOf("Historia de vida: ") + "Historia de vida: ".length();
        return prompt.substring(inicio, prompt.indexOf('\n', inicio)).strip();
    }

    private static int lineasDeRecuerdos(String recuerdos) {
        return (int) PASO_DE_ORDEN.matcher(recuerdos).results().count();
    }

    // ---------- P13: por persona ----------

    @Test
    void unNombreDePersonaEnElMensajeTraeSusRecuerdos() throws Exception {
        elena(personaA, 1);
        siestas(personaA, 2);

        String prompt = prompt(tokenA, "Hoy me acordé de Elena");

        assertThat(recuerdosDelPrompt(prompt)).isEqualTo(LINEA_ELENA);
    }

    @Test
    void elNombreSeDetectaSinImportarMayusculasTildesNiPuntuacion() throws Exception {
        elena(personaA, 1);
        siestas(personaA, 2);

        assertThat(recuerdosDelPrompt(prompt(tokenA, "¡ELENA! Qué recuerdos."))).isEqualTo(LINEA_ELENA);
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Hablé de Rosa con mi hija"))).isEqualTo(LINEA_SIESTAS);
    }

    @Test
    void unaPersonaConocidaSoloPorElVinculoNoSeBuscaPorNombre() throws Exception {
        // Solo se conoce el vínculo ("marido"): P16 la filtra, no hay nada que mencionar por nombre.
        String id = recuerdos.crear(personaA.personaId(), 1, "Un viaje", 1980, "EXACTA", 0, 0, 0, 1);
        recuerdos.descripcion(id, "Viajaron juntos.");
        recuerdos.agregarPersona(personaA.personaId(), id, null, "marido");

        assertThat(recuerdosDelPrompt(prompt(tokenA, "Me acordé de mi marido"))).isEqualTo(SIN_INFORMACION);
    }

    // ---------- P14: por lugar ----------

    @Test
    void unLugarConPuntuacionSeDetectaComoEscuelaN12OComoEscuelaN12ConGrado() throws Exception {
        elena(personaA, 1);
        siestas(personaA, 2);

        // El nombre guardado es "escuela n° 12"; el mensaje normalizado no tiene el °.
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Estuve pensando en la escuela n 12"))).isEqualTo(LINEA_ELENA);
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Estuve pensando en la Escuela N° 12"))).isEqualTo(LINEA_ELENA);
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Fui a la Escuela N°12 de chiquita"))).contains(LINEA_ELENA);
    }

    @Test
    void unLugarDeVariasPalabrasSeDetectaSoloCompleto() throws Exception {
        siestas(personaA, 1);

        assertThat(recuerdosDelPrompt(prompt(tokenA, "Me acuerdo de la casa de la abuela"))).isEqualTo(LINEA_SIESTAS);
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Me acuerdo de la casa"))).isEqualTo(SIN_INFORMACION);
    }

    // ---------- P15: por etapa ----------

    @Test
    void unaFraseDeEtapaTraeLosRecuerdosDeEsaEtapa() throws Exception {
        elena(personaA, 1);
        siestas(personaA, 2);
        carlos(personaA, 3);

        // INFANCIA: los dos de infancia, el más reciente primero. No el de JUVENTUD.
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Me acuerdo de cuando era chica")))
                .isEqualTo(LINEA_SIESTAS + "\n" + LINEA_ELENA);
        // JUVENTUD: solo Carlos.
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Me acuerdo de cuando era joven"))).isEqualTo(LINEA_CARLOS);
    }

    @Test
    void lasFrasesDeEtapaSeNormalizanYSeLeenDeLaConfiguracion() throws Exception {
        elena(personaA, 1);
        carlos(personaA, 2);

        assertThat(recuerdosDelPrompt(prompt(tokenA, "Cuando era NIÑA jugaba mucho"))).isEqualTo(LINEA_ELENA);
        assertThat(recuerdosDelPrompt(prompt(tokenA, "De joven bailaba tango"))).isEqualTo(LINEA_CARLOS);
        // Una etapa sin frases (VEJEZ) y un recuerdo de esa etapa: nada para traer.
        String vejez = recuerdos.crear(personaA.personaId(), 1, "Ahora", 2020, "EXACTA", 0, 0, 0, 3);
        recuerdos.etapa(vejez, "VEJEZ");
        assertThat(recuerdosDelPrompt(prompt(tokenA, "Estoy en la vejez"))).isEqualTo(SIN_INFORMACION);
    }

    // ---------- Orden y máximo ----------

    @Test
    void conPersonaYEtapaPrimeroVaLaPersonaYElTotalNoPasaDe3() throws Exception {
        elena(personaA, 1);
        siestas(personaA, 2);
        for (int i = 1; i <= 2; i++) {
            String extra = recuerdos.crear(personaA.personaId(), 1, "Extra " + i, 1950 + i, "EXACTA", 0, 0, 0,
                    10 + i);
            recuerdos.descripcion(extra, "Descripción " + i + ".");
        }

        String sección = recuerdosDelPrompt(prompt(tokenA, "Elena, de chica"));

        // INFANCIA tiene 4 (Elena, Siestas, Extra 1 y 2). Elena (por persona) va primero y después, los más
        // recientes de la etapa: Extra 2 y Extra 1. Las siestas quedan afuera: máximo 3.
        assertThat(lineasDeRecuerdos(sección)).isEqualTo(3);
        assertThat(sección.lines().toList()).containsExactly(LINEA_ELENA,
                "- Extra 2 (1952): Descripción 2.", "- Extra 1 (1951): Descripción 1.");
    }

    @Test
    void unRecuerdoQueCoincidePorPersonaYPorEtapaNoSeRepite() throws Exception {
        elena(personaA, 1);

        assertThat(recuerdosDelPrompt(prompt(tokenA, "Elena, cuando era chica y la Escuela N° 12")))
                .isEqualTo(LINEA_ELENA);
    }

    // ---------- Límites del prompt ----------

    @Test
    void laDescripcionSeCortaA300ConPuntosSuspensivos() throws Exception {
        String id = elena(personaA, 1);
        recuerdos.descripcion(id, "a".repeat(400));

        String sección = recuerdosDelPrompt(prompt(tokenA, "Elena"));

        assertThat(sección).contains("a".repeat(300) + "…").doesNotContain("a".repeat(301));
    }

    @Test
    void unRecuerdoConMarcadoresEnElTextoNoSeVuelveAReemplazar() throws Exception {
        String id = elena(personaA, 1);
        recuerdos.descripcion(id, "Dijo {animo} y {progreso} y {recordatorios} y {citas_proximas}.");

        String prompt = prompt(tokenA, "Elena");

        assertThat(recuerdosDelPrompt(prompt))
                .contains("Dijo {animo} y {progreso} y {recordatorios} y {citas_proximas}.");
    }

    // ---------- Progreso ----------

    @Test
    void elProgresoLleva3CantidadesYSinRespuestasQuedaSinInformacion() throws Exception {
        assertThat(progresoDelPrompt(prompt(tokenA, "Hola"))).isEqualTo(SIN_INFORMACION);

        enviar(tokenA, "/api/historia/guion/1/respuesta", "{\"texto\":\"Nací en un pueblo.\"}", 202);
        enviar(tokenA, "/api/historia/guion/2/respuesta", "{\"texto\":\"Vivía con mi madre.\"}", 202);
        enviar(tokenA, "/api/historia/guion/3/salteo", null, 204);
        esperarCola();
        int pendientes = ((Number) com.jayway.jsonpath.JsonPath.read(mockMvc.perform(
                con(get("/api/historia/guion/estado"), tokenA)).andReturn().getResponse().getContentAsString(),
                "$.pendientes")).intValue();
        modelo.reiniciar();

        assertThat(progresoDelPrompt(prompt(tokenA, "Hola"))).isEqualTo("Respondió 2, salteó 1, quedan " + pendientes + ".");
        assertThat(pendientes).isEqualTo(18);
    }

    private void enviar(String token, String ruta, String cuerpo, int esperado) throws Exception {
        MockHttpServletRequestBuilder pedido = con(post(ruta), token);
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        mockMvc.perform(pedido).andExpect(status().is(esperado));
    }

    // ---------- Sin información, sin nodo y aislamiento ----------

    @Test
    void sinCoincidenciasNoHayInformacion() throws Exception {
        elena(personaA, 1);

        assertThat(recuerdosDelPrompt(prompt(tokenA, "Hoy desayuné mate con tortas fritas"))).isEqualTo(SIN_INFORMACION);
    }

    @Test
    void unaPersonaSinNodoEnElGrafoQuedaSinInformacionYElChatNoLoCrea() throws Exception {
        assertThat(contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p",
                personaB.personaId())).isZero();

        String prompt = prompt(tokenB, "Hola, me acordé de Elena de chica");

        assertThat(progresoDelPrompt(prompt)).isEqualTo(SIN_INFORMACION);
        assertThat(recuerdosDelPrompt(prompt)).isEqualTo(SIN_INFORMACION);
        assertThat(contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p",
                personaB.personaId())).as("leer para el chat no crea el nodo de la persona").isZero();
    }

    @Test
    void losRecuerdosDeOtraPersonaNuncaAparecen() throws Exception {
        // A tiene su "Elena" y su "Escuela N° 12". B tiene los mismos nombres, la misma etapa y la misma persona
        // con otro recuerdo: P13 (por persona), P14 (por lugar) y P15 (por etapa) se ejecutan de verdad con datos
        // de las dos personas, y cada una tiene que ver solo lo suyo.
        elena(personaA, 1);
        String deB = recuerdos.crear(personaB.personaId(), 1, "La Elena de B", 1960, "EXACTA", 0, 0, 0, 5);
        recuerdos.descripcion(deB, "Secreto de B.");
        recuerdos.agregarPersona(personaB.personaId(), deB, "Elena", "vecina");
        recuerdos.agregarLugar(deB, "Escuela N° 12");
        recuerdos.descripcion(carlos(personaB, 6), "Otro secreto de B.");
        String lineaDeB = "- La Elena de B (1960): Secreto de B. Con Elena (vecina), en Escuela N° 12.";

        // Por persona (P13), por lugar (P14) y por etapa (P15), cada una por separado.
        for (String mensaje : List.of("Hoy me acordé de Elena", "Estuve en la Escuela N° 12", "Cuando era chica")) {
            String paraA = prompt(tokenA, mensaje);
            assertThat(recuerdosDelPrompt(paraA)).as("A con: " + mensaje).isEqualTo(LINEA_ELENA);
            assertThat(paraA).doesNotContain("Secreto de B").doesNotContain("La Elena de B");
        }
        // Y la de A tampoco aparece en el chat de B: cada uno ve solo lo suyo.
        for (String mensaje : List.of("Hoy me acordé de Elena", "Estuve en la Escuela N° 12")) {
            String paraB = prompt(tokenB, mensaje);
            assertThat(recuerdosDelPrompt(paraB)).as("B con: " + mensaje).isEqualTo(lineaDeB);
            assertThat(paraB).doesNotContain("Mi maestra Elena");
        }
        assertThat(recuerdosDelPrompt(prompt(tokenB, "Cuando era chica"))).isEqualTo(lineaDeB);
    }

    @Test
    void elPromptNoTraeMarcadoresSinReemplazar() throws Exception {
        elena(personaA, 1);

        String prompt = prompt(tokenA, "Elena");

        assertThat(Pattern.compile("\\{[a-z_]+}").matcher(prompt).find()).as("marcadores sin reemplazar").isFalse();
    }
}
