package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Extracción de punta a punta (contrato del grafo, secciones 5.1, 5.4 a 5.6 y 11): responder encola, la cola
 * llama al extractor con el modelo de prueba y se escribe el recuerdo en el Neo4j de {@code docker compose up
 * -d}. Persona de prueba: "Test Historia", nacida el 12/05/1950. Cada prueba espera a que la cola se vacíe
 * antes de limpiar.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@Import(ModeloDePruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ExtraccionIntegrationTests {

    private static final String USUARIO = "test_historia_ext_a";
    private static final String OTRO = "test_historia_ext_b";
    private static final List<String> USUARIOS = List.of(USUARIO, OTRO);
    private static final Duration ESPERA = Duration.ofSeconds(20);

    /** Pregunta 7: franja de 6 a 12 años (estima 1959). Pregunta 15: sin franja. */
    private static final int ESCUELA = 7;
    private static final int CASA = 2;
    private static final int HIJOS = 15;

    private static final String VALIDO = """
            {"titulo":"Mi maestra Elena","descripcion":"Su maestra de primer grado.","anio":1957,\
            "anioAproximado":false,"edadMencionada":null,"personas":[{"nombre":"Elena","vinculo":"maestra"}],\
            "lugares":["Escuela N° 12"]}""";

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
    private Persona persona;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        esperarCola();
        modelo.reiniciar();
        USUARIOS.forEach(personas::borrar);
        persona = personas.crear(USUARIO);
    }

    @AfterEach
    void limpiar() {
        esperarCola();
        USUARIOS.forEach(personas::borrar);
        modelo.reiniciar();
    }

    private void esperarCola() {
        assertThat(cola.esperarQueSeVacie(ESPERA)).as("la cola se vació").isTrue();
    }

    private void responderYEsperar(Persona quien, int pregunta, String texto) {
        historia.responder(quien.cuentaId(), pregunta, texto);
        esperarCola();
    }

    private List<Map<String, Object>> recuerdos(Persona quien) {
        return filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
                RETURN r.titulo AS titulo, r.descripcion AS descripcion, r.anio AS anio, r.precision AS precision,
                       r.etapaDeVida AS etapa, r.intentosAnio AS intentosAnio, r.intentosLugares AS intentosLugares,
                       r.intentosPersonas AS intentosPersonas, q.idPreguntaGuion AS pregunta
                ORDER BY r.fechaRegistro
                """, "p", quien.personaId());
    }

    private List<Map<String, Object>> personasConocidas(Persona quien) {
        return filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $p})-[c:CONOCE]->(pe:Persona)
                RETURN pe.nombre AS nombre, pe.nombreNormalizado AS normalizado, c.vinculo AS vinculo,
                       size([(pe)-[:PARTICIPA_EN]->(:Recuerdo) | 1]) AS recuerdos
                ORDER BY pe.nombre
                """, "p", quien.personaId());
    }

    private long lugares(Persona quien) {
        return contar(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(l:Lugar)
                RETURN count(DISTINCT l) AS c
                """, "p", quien.personaId());
    }

    // ---------- Recuerdo completo ----------

    @Test
    void unaRespuestaValidaCreaElRecuerdoConSusRelacionesYLosContadoresEnCero() {
        modelo.responde(VALIDO);

        responderYEsperar(persona, ESCUELA, "En 1957 empecé primer grado en la Escuela N° 12. Mi maestra Elena me enseñó a leer.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> {
            assertThat(r).containsEntry("titulo", "Mi maestra Elena").containsEntry("pregunta", (long) ESCUELA)
                    .containsEntry("anio", 1957L).containsEntry("precision", "EXACTA")
                    .containsEntry("etapa", "INFANCIA").containsEntry("intentosAnio", 0L)
                    .containsEntry("intentosLugares", 0L).containsEntry("intentosPersonas", 0L);
        });
        assertThat(personasConocidas(persona)).singleElement().satisfies(p -> assertThat(p)
                .containsEntry("nombre", "Elena").containsEntry("vinculo", "maestra").containsEntry("recuerdos", 1L));
        assertThat(lugares(persona)).isEqualTo(1);
        // La pregunta quedó respondida (de inmediato) y ahora la próxima es otra.
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO]->(:PreguntaGuion {idPreguntaGuion: 7}) RETURN count(r) AS c",
                "p", persona.personaId())).isEqualTo(1);
    }

    @Test
    void responderDevuelveSinEsperarAlModeloYLaPreguntaQuedaRespondidaAlInstante() {
        modelo.respondeConDemora(VALIDO, 600);

        long inicio = System.nanoTime();
        historia.responder(persona.cuentaId(), ESCUELA, "Una respuesta.");
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(ms).as("responder no espera a la extracción").isLessThan(500);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO]->() RETURN count(r) AS c",
                "p", persona.personaId())).isEqualTo(1);
        esperarCola();
        assertThat(recuerdos(persona)).hasSize(1);
    }

    // ---------- Año ----------

    @Test
    void sinAnioDelModeloSeEstimaPorLaFranjaDeLaPregunta() {
        responderYEsperar(persona, ESCUELA, "Me acuerdo del guardapolvo blanco.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("anio", 1959L).containsEntry("precision", "ESTIMADA").containsEntry("etapa", "INFANCIA"));
    }

    @Test
    void sinAnioYSinFranjaElRecuerdoQuedaSinAnioPrecisionNiEtapa() {
        responderYEsperar(persona, HIJOS, "Fue un día hermoso.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("anio", null).containsEntry("precision", null).containsEntry("etapa", null));
    }

    @Test
    void conEdadMencionadaSeCalculaElAnio() {
        modelo.responde(VALIDO.replace("\"anio\":1957", "\"anio\":null").replace("\"edadMencionada\":null",
                "\"edadMencionada\":17"));

        responderYEsperar(persona, ESCUELA, "Trabajé en una panadería cuando tenía 17 años.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r).containsEntry("anio", 1967L)
                .containsEntry("precision", "CALCULADA_POR_EDAD").containsEntry("etapa", "ADOLESCENCIA"));
    }

    @Test
    void unAnioAnteriorAlNacimientoSeDescartaYSigueConLaFranja() {
        modelo.responde(VALIDO.replace("1957", "1940"));

        responderYEsperar(persona, ESCUELA, "Fue en 1940.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("anio", 1959L).containsEntry("precision", "ESTIMADA"));
    }

    @Test
    void unaDecadaEscritaQuedaEstimadaPorLaFranjaAunqueElModeloLaConvierta() {
        modelo.responde(VALIDO.replace("1957", "1950").replace("\"anioAproximado\":false", "\"anioAproximado\":true"));

        responderYEsperar(persona, CASA, "Por los años 50, en Cipolletti, en una casa con patio.");

        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("anio", 1955L).containsEntry("precision", "ESTIMADA"));
    }

    // ---------- Respaldo ----------

    @Test
    void siFallaElModeloSeCreaElRecuerdoDeRespaldoYNoHayErrorHaciaAfuera() {
        modelo.falla();

        responderYEsperar(persona, ESCUELA, "Me acuerdo del patio y de mis amigas.");

        String preguntaTexto = (String) filas(neo4j, "MATCH (q:PreguntaGuion {idPreguntaGuion: 7}) RETURN q.texto AS t")
                .get(0).get("t");
        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("titulo", preguntaTexto).containsEntry("descripcion", "Me acuerdo del patio y de mis amigas.")
                .containsEntry("anio", 1959L).containsEntry("precision", "ESTIMADA").containsEntry("intentosAnio", 0L));
        assertThat(personasConocidas(persona)).isEmpty();
        assertThat(lugares(persona)).isZero();
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO]->() RETURN count(r) AS c",
                "p", persona.personaId())).isEqualTo(1);
    }

    @Test
    void conElJsonInvalidoDosVecesSeCreaElRespaldo() {
        modelo.responde("no es json").responde("sigue sin serlo");

        responderYEsperar(persona, HIJOS, "Nació en el hospital.");

        assertThat(modelo.pedidos()).hasSize(2);
        assertThat(recuerdos(persona)).singleElement().satisfies(r -> assertThat(r)
                .containsEntry("descripcion", "Nació en el hospital.").containsEntry("anio", null));
    }

    // ---------- Sin duplicados ----------

    @Test
    void dosRespuestasConMartaYCipollettiDejanUnaSolaPersonaYUnSoloLugar() {
        modelo.responde(recuerdoCon("[{\"nombre\":\"Marta\",\"vinculo\":\"hermana\"}]", "[\"Cipolletti\"]"))
                .responde(recuerdoCon("[{\"nombre\":\"MARTA\",\"vinculo\":\"amiga\"}]", "[\"cipolletti\"]"));

        responderYEsperar(persona, CASA, "Con Marta en Cipolletti.");
        responderYEsperar(persona, 3, "Con MARTA en cipolletti.");

        assertThat(recuerdos(persona)).hasSize(2);
        assertThat(personasConocidas(persona)).singleElement().satisfies(p -> assertThat(p)
                .containsEntry("nombre", "Marta").containsEntry("vinculo", "hermana")
                .containsEntry("recuerdos", 2L));
        assertThat(lugares(persona)).isEqualTo(1);
        assertThat(contar(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(:Recuerdo)-[o:OCURRIO_EN]->(:Lugar)
                RETURN count(o) AS c
                """, "p", persona.personaId())).isEqualTo(2);
    }

    @Test
    void repetirLaMismaPersonaOElMismoLugarEnUnaRespuestaTampocoDuplica() {
        modelo.responde(recuerdoCon("[{\"nombre\":\"Marta\",\"vinculo\":\"hermana\"},{\"nombre\":\"marta\",\"vinculo\":\"hermana\"}]",
                "[\"Allen\",\"ALLEN\",\"Cipolletti\"]"));

        responderYEsperar(persona, CASA, "Marta, marta, Allen y Cipolletti.");

        assertThat(personasConocidas(persona)).hasSize(1);
        assertThat(lugares(persona)).isEqualTo(2);
    }

    @Test
    void unaPersonaSinNombreConUnVinculoUnicoSeReutilizaYConDosDelMismoVinculoSeCreaOtra() {
        modelo.responde(recuerdoCon("[{\"nombre\":null,\"vinculo\":\"tío\"}]", "[]"))
                .responde(recuerdoCon("[{\"nombre\":null,\"vinculo\":\"tío\"}]", "[]"))
                .responde(recuerdoCon("[{\"nombre\":\"Juan\",\"vinculo\":\"tío\"}]", "[]"))
                .responde(recuerdoCon("[{\"nombre\":null,\"vinculo\":\"tío\"}]", "[]"));

        responderYEsperar(persona, CASA, "Mi tío.");
        responderYEsperar(persona, 3, "Mi tío otra vez.");
        // Una sola persona con ese vínculo: la segunda respuesta la reutilizó.
        assertThat(personasConocidas(persona)).singleElement().satisfies(p -> assertThat(p)
                .containsEntry("recuerdos", 2L));

        responderYEsperar(persona, 4, "Mi tío Juan.");
        // Ahora hay dos personas con el vínculo "tío": ya no se puede saber cuál es.
        responderYEsperar(persona, 5, "Mi tío.");

        assertThat(personasConocidas(persona)).hasSize(3);
    }

    @Test
    void laPropiaPersonaMayorNoEntraComoPersona() {
        modelo.responde(recuerdoCon("[{\"nombre\":\"Test\",\"vinculo\":\"yo\"},{\"nombre\":\"Rosa\",\"vinculo\":\"amiga\"}]", "[]"));

        responderYEsperar(persona, 10, "Me llamo Test y salía con mi amiga Rosa.");

        assertThat(personasConocidas(persona)).extracting(p -> p.get("nombre")).containsExactly("Rosa");
    }

    // ---------- Aislamiento ----------

    @Test
    void dosPersonasConLaMismaPersonaYElMismoLugarNoSeMezclan() {
        Persona otra = personas.crear(OTRO);
        modelo.responde(recuerdoCon("[{\"nombre\":\"Marta\",\"vinculo\":\"hermana\"}]", "[\"Allen\"]"))
                .responde(recuerdoCon("[{\"nombre\":\"Marta\",\"vinculo\":\"prima\"}]", "[\"Allen\"]"));

        responderYEsperar(persona, CASA, "Marta en Allen.");
        responderYEsperar(otra, CASA, "Marta en Allen.");

        assertThat(personasConocidas(persona)).singleElement().satisfies(p -> assertThat(p).containsEntry("vinculo", "hermana"));
        assertThat(personasConocidas(otra)).singleElement().satisfies(p -> assertThat(p).containsEntry("vinculo", "prima"));
        assertThat(lugares(persona)).isEqualTo(1);
        assertThat(lugares(otra)).isEqualTo(1);
    }

    // ---------- Cola ----------

    @Test
    void conVariasExtraccionesSeAtiendenDeAUnaYEnOrdenDeLlegada() {
        modelo.respondeConDemora(VALIDO.replace("Mi maestra Elena", "Primero"), 150)
                .respondeConDemora(VALIDO.replace("Mi maestra Elena", "Segundo"), 50)
                .respondeConDemora(VALIDO.replace("Mi maestra Elena", "Tercero"), 10);

        historia.responder(persona.cuentaId(), 1, "Respuesta uno.");
        historia.responder(persona.cuentaId(), 2, "Respuesta dos.");
        historia.responder(persona.cuentaId(), 3, "Respuesta tres.");
        esperarCola();

        assertThat(modelo.maximoSimultaneo()).isEqualTo(1);
        assertThat(modelo.pedidos()).extracting(ModeloDePrueba.Pedido::mensaje)
                .allSatisfy(m -> assertThat(m).contains("Respuesta"))
                .satisfies(m -> assertThat(m.get(0)).contains("uno"))
                .satisfies(m -> assertThat(m.get(1)).contains("dos"))
                .satisfies(m -> assertThat(m.get(2)).contains("tres"));
        assertThat(recuerdos(persona)).extracting(r -> r.get("titulo")).containsExactly("Primero", "Segundo", "Tercero");
    }

    @Test
    void esperarQueSeVacieDevuelveFalseSiSeAcabaElTiempoYTrueCuandoTermina() {
        modelo.respondeConDemora(VALIDO, 500);
        historia.responder(persona.cuentaId(), ESCUELA, "Una respuesta.");

        assertThat(cola.esperarQueSeVacie(Duration.ofMillis(50))).isFalse();
        assertThat(cola.esperarQueSeVacie(ESPERA)).isTrue();
    }

    @Test
    void siLaPersonaSeDaDeBajaMientrasTantoLaExtraccionTerminaSinErrorYSinEscribirNada() {
        modelo.respondeConDemora(VALIDO, 500);
        historia.responder(persona.cuentaId(), ESCUELA, "Una respuesta.");

        personas.borrar(USUARIO);
        esperarCola();

        assertThat(contar(neo4j, "MATCH (r:Recuerdo) WHERE r.titulo = 'Mi maestra Elena' AND NOT EXISTS { (:PersonaMayor)-[:TIENE_RECUERDO]->(r) } RETURN count(r) AS c"))
                .as("ningún recuerdo huérfano").isZero();
        assertThat(contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p", persona.personaId()))
                .isZero();
    }

    // ---------- Lo que NO encola ----------

    @Test
    void unaRespuestaInvalidaOUnSalteoNoEncolanNiTocanElGrafoDeRecuerdos() {
        assertThatThrownBy(() -> historia.responder(persona.cuentaId(), ESCUELA, "x".repeat(1001)))
                .isInstanceOf(DatosInvalidosException.class);
        historia.saltear(persona.cuentaId(), CASA);
        esperarCola();

        assertThat(modelo.pedidos()).isEmpty();
        assertThat(recuerdos(persona)).isEmpty();
    }

    @Test
    void responderDosVecesLaMismaPreguntaEncolaUnaSolaVez() {
        historia.responder(persona.cuentaId(), ESCUELA, "Una respuesta.");
        assertThatThrownBy(() -> historia.responder(persona.cuentaId(), ESCUELA, "Otra."))
                .isInstanceOf(ar.edu.utn.frm.gelab.alma.common.exception.ConflictoException.class);
        esperarCola();

        assertThat(modelo.pedidos()).hasSize(1);
        assertThat(recuerdos(persona)).hasSize(1);
    }

    // ---------- Privacidad (5.9) ----------

    @Test
    void ningunLogContieneElTextoDeLaRespuestaNiLosNombres(CapturedOutput salida) {
        modelo.responde(recuerdoCon("[{\"nombre\":\"NOMBRE_SECRETO\",\"vinculo\":\"amiga\"}]", "[\"LUGAR_SECRETO\"]"));
        responderYEsperar(persona, CASA, "TEXTO_SECRETO con NOMBRE_SECRETO en LUGAR_SECRETO.");
        modelo.falla();
        responderYEsperar(persona, 3, "OTRO_TEXTO_SECRETO.");

        assertThat(salida.getAll()).doesNotContain("TEXTO_SECRETO").doesNotContain("NOMBRE_SECRETO")
                .doesNotContain("LUGAR_SECRETO");
    }

    // ---------- Utilidades ----------

    private static String recuerdoCon(String personas, String lugares) {
        return "{\"titulo\":\"Un recuerdo\",\"descripcion\":\"Contó algo.\",\"anio\":null,\"anioAproximado\":false,"
                + "\"edadMencionada\":null,\"personas\":" + personas + ",\"lugares\":" + lugares + "}";
    }
}
