package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Lectura de recuerdos (contrato de historia de vida, 5.1, 5.2, 7.3 y 7.4): orden de la línea de tiempo, detalle,
 * 404 y aislamiento entre personas, con {@code MockMvc} contra el Postgres y el Neo4j de
 * {@code docker compose up -d}. Los recuerdos se arman directo en el grafo. Pregunta 1 del guion real.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(ModeloDePruebaConfig.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ConsultaDeRecuerdosIntegrationTests {

    private static final String RECUERDOS = "/api/historia/recuerdos";
    private static final String USUARIO_A = "test_historia_rec_a";
    private static final String USUARIO_B = "test_historia_rec_b";
    private static final String SIN_PERSONA = "test_historia_rec_sin_persona";
    private static final List<String> USUARIOS = List.of(USUARIO_A, USUARIO_B, SIN_PERSONA);
    private static final int PREGUNTA = 1;
    private static final String NO_ENCONTRADO = "No encontramos lo que buscás.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HistoriaDeVidaService historia;
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
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        recuerdos = new RecuerdosDePrueba(neo4j);
        USUARIOS.forEach(personas::borrar);
        personaA = personas.crear(USUARIO_A);
        personaB = personas.crear(USUARIO_B);
        tokenA = personas.token(personaA.cuentaId(), "PERSONA_MAYOR");
        tokenB = personas.token(personaB.cuentaId(), "PERSONA_MAYOR");
        tokenAdmin = personas.token(999_999L, "ADMINISTRADOR");
    }

    @AfterEach
    void limpiar() {
        USUARIOS.forEach(personas::borrar);
    }

    private static MockHttpServletRequestBuilder con(MockHttpServletRequestBuilder pedido, String token) {
        return pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String recuerdo(Persona quien, String titulo, Integer anio, String precision, int minutos) {
        return recuerdos.crear(quien.personaId(), PREGUNTA, titulo, anio, precision, 0, 0, 0, minutos);
    }

    // ---------- Acceso ----------

    @Test
    void sinTokenDevuelve401EnLosDosEndpoints() throws Exception {
        mockMvc.perform(get(RECUERDOS)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(RECUERDOS + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void unAdministradorRecibe403EnLosDosEndpoints() throws Exception {
        mockMvc.perform(con(get(RECUERDOS), tokenAdmin)).andExpect(status().isForbidden());
        mockMvc.perform(con(get(RECUERDOS + "/" + UUID.randomUUID()), tokenAdmin)).andExpect(status().isForbidden());
    }

    @Test
    void unaCuentaSinPersonaMayorRecibe404EnLosDosEndpoints() throws Exception {
        String token = personas.token(personas.crearCuentaSinPersona(SIN_PERSONA), "PERSONA_MAYOR");
        mockMvc.perform(con(get(RECUERDOS), token)).andExpect(status().isNotFound());
        mockMvc.perform(con(get(RECUERDOS + "/" + UUID.randomUUID()), token)).andExpect(status().isNotFound());
    }

    // ---------- Listado ----------

    @Test
    void sinRecuerdosDevuelveListaVaciaConStatus200() throws Exception {
        mockMvc.perform(con(get(RECUERDOS), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recuerdos", hasSize(0)));
    }

    @Test
    void ordenaPorAnioEmpataPorFechaDeRegistroYDejaLosSinAnioAlFinal() throws Exception {
        recuerdo(personaA, "sin año tardío", null, null, 4);
        recuerdo(personaA, "1960 tardío", 1960, "EXACTA", 5);
        recuerdo(personaA, "1950", 1950, "APROXIMADA", 3);
        recuerdo(personaA, "sin año temprano", null, null, 2);
        recuerdo(personaA, "1960 temprano", 1960, "EXACTA", 1);

        mockMvc.perform(con(get(RECUERDOS), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recuerdos", hasSize(5)))
                .andExpect(jsonPath("$.recuerdos[0].titulo").value("1950"))
                .andExpect(jsonPath("$.recuerdos[1].titulo").value("1960 temprano"))
                .andExpect(jsonPath("$.recuerdos[2].titulo").value("1960 tardío"))
                .andExpect(jsonPath("$.recuerdos[3].titulo").value("sin año temprano"))
                .andExpect(jsonPath("$.recuerdos[4].titulo").value("sin año tardío"));
    }

    @Test
    void elListadoTraeLosCamposDeLaLineaDeTiempoYUnResumenDe140() throws Exception {
        String conAnio = recuerdo(personaA, "Mi maestra Elena", 1957, "APROXIMADA", 1);
        recuerdos.descripcion(conAnio, "a".repeat(200));
        String sinAnio = recuerdo(personaA, "Conocí a Carlos", null, null, 2);
        recuerdos.descripcion(sinAnio, "Corta.");

        mockMvc.perform(con(get(RECUERDOS), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recuerdos[0].id").value(conAnio))
                .andExpect(jsonPath("$.recuerdos[0].anio").value(1957))
                .andExpect(jsonPath("$.recuerdos[0].precision").value("APROXIMADA"))
                .andExpect(jsonPath("$.recuerdos[0].etapaDeVida").value("INFANCIA"))
                .andExpect(jsonPath("$.recuerdos[0].resumen").value("a".repeat(140) + "…"))
                .andExpect(jsonPath("$.recuerdos[0].descripcion").doesNotExist())
                .andExpect(jsonPath("$.recuerdos[1].anio").value(nullValue()))
                .andExpect(jsonPath("$.recuerdos[1].precision").value(nullValue()))
                .andExpect(jsonPath("$.recuerdos[1].etapaDeVida").value(nullValue()))
                .andExpect(jsonPath("$.recuerdos[1].resumen").value("Corta."));
    }

    @Test
    void cadaPersonaVeSoloSusRecuerdos() throws Exception {
        recuerdo(personaA, "De A", 1950, "EXACTA", 1);
        recuerdo(personaB, "De B", 1951, "EXACTA", 1);

        mockMvc.perform(con(get(RECUERDOS), tokenA))
                .andExpect(jsonPath("$.recuerdos", hasSize(1)))
                .andExpect(jsonPath("$.recuerdos[0].titulo").value("De A"));
        mockMvc.perform(con(get(RECUERDOS), tokenB))
                .andExpect(jsonPath("$.recuerdos", hasSize(1)))
                .andExpect(jsonPath("$.recuerdos[0].titulo").value("De B"));
    }

    @Test
    void unValorInvalidoDePrecisionOEtapaEnElGrafoNoTumbaNiElListadoNiElDetalle() throws Exception {
        String raro = recuerdo(personaA, "Cargado a mano", 1950, "RARA", 1);
        recuerdos.etapa(raro, "XYZ");
        String normal = recuerdo(personaA, "Normal", 1960, "EXACTA", 2);

        mockMvc.perform(con(get(RECUERDOS), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recuerdos", hasSize(2)))
                .andExpect(jsonPath("$.recuerdos[0].id").value(raro))
                .andExpect(jsonPath("$.recuerdos[0].anio").value(1950))
                .andExpect(jsonPath("$.recuerdos[0].precision").value(nullValue()))
                .andExpect(jsonPath("$.recuerdos[0].etapaDeVida").value(nullValue()))
                .andExpect(jsonPath("$.recuerdos[1].id").value(normal))
                .andExpect(jsonPath("$.recuerdos[1].precision").value("EXACTA"));
        mockMvc.perform(con(get(RECUERDOS + "/" + raro), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Cargado a mano"))
                .andExpect(jsonPath("$.precision").value(nullValue()))
                .andExpect(jsonPath("$.etapaDeVida").value(nullValue()));
    }

    // ---------- Detalle ----------

    @Test
    void elDetalleTraePersonasConVinculoYLugares() throws Exception {
        String id = recuerdo(personaA, "Mi maestra Elena", 1957, "APROXIMADA", 1);
        recuerdos.descripcion(id, "Su maestra de primer grado, que le enseñó a leer.");
        recuerdos.agregarPersona(personaA.personaId(), id, "Elena", "maestra");
        recuerdos.agregarPersona(personaA.personaId(), id, null, "marido");
        recuerdos.agregarLugar(id, "Escuela N° 12");

        mockMvc.perform(con(get(RECUERDOS + "/" + id), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Mi maestra Elena"))
                .andExpect(jsonPath("$.descripcion").value("Su maestra de primer grado, que le enseñó a leer."))
                .andExpect(jsonPath("$.anio").value(1957))
                .andExpect(jsonPath("$.precision").value("APROXIMADA"))
                .andExpect(jsonPath("$.etapaDeVida").value("INFANCIA"))
                .andExpect(jsonPath("$.personas", hasSize(2)))
                .andExpect(jsonPath("$.personas[?(@.nombre == 'Elena')].vinculo").value("maestra"))
                .andExpect(jsonPath("$.personas[?(@.nombre == null)].vinculo").value("marido"))
                .andExpect(jsonPath("$.lugares", hasSize(1)))
                .andExpect(jsonPath("$.lugares[0].nombre").value("Escuela N° 12"));
    }

    @Test
    void sinPersonasNiLugaresDevuelveListasVaciasYSinAnioCamposNulos() throws Exception {
        String id = recuerdo(personaA, "Un recuerdo", null, null, 1);

        mockMvc.perform(con(get(RECUERDOS + "/" + id), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(nullValue()))
                .andExpect(jsonPath("$.precision").value(nullValue()))
                .andExpect(jsonPath("$.etapaDeVida").value(nullValue()))
                .andExpect(jsonPath("$.personas", hasSize(0)))
                .andExpect(jsonPath("$.lugares", hasSize(0)));
    }

    @Test
    void unRecuerdoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(con(get(RECUERDOS + "/" + UUID.randomUUID()), tokenA))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(NO_ENCONTRADO)));
    }

    @Test
    void unRecuerdoDeOtraPersonaDevuelve404IgualQueUnoInexistente() throws Exception {
        String deB = recuerdo(personaB, "De B", 1951, "EXACTA", 1);
        recuerdos.agregarLugar(deB, "Un lugar de B");

        String ajeno = mockMvc.perform(con(get(RECUERDOS + "/" + deB), tokenA))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String inexistente = mockMvc.perform(con(get(RECUERDOS + "/" + UUID.randomUUID()), tokenA))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        assertThat(ajeno).doesNotContain("De B").doesNotContain("Un lugar de B");
        assertThat(ajeno.replaceAll("\"instance\":\"[^\"]*\"", "")).isEqualTo(inexistente.replaceAll("\"instance\":\"[^\"]*\"", ""));
        // Y B sí lo ve.
        mockMvc.perform(con(get(RECUERDOS + "/" + deB), tokenB)).andExpect(status().isOk());
    }
}
