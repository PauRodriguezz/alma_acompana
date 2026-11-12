package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;
import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.ANIO;
import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.LUGARES;
import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.PERSONAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.common.exception.ConflictoException;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoRepregunta;
import ar.edu.utn.frm.gelab.alma.historia.dto.RepreguntaSiguienteResponse;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.historia.service.RepreguntaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Las repreguntas (contrato del grafo, 5.7 y 11) a nivel de servicio, contra el Neo4j de {@code docker compose
 * up -d} y con el modelo de prueba. Los recuerdos se arman directo en el grafo, con la fecha, el año y los
 * intentos que cada caso necesita. Persona: "Test Historia", nacida el 12/05/1950.
 *
 * <p>Preguntas del guion usadas (lista {@code repreguntas}): la 1 {@code []}, la 2 {@code [LUGARES, PERSONAS]},
 * la 3 {@code [PERSONAS]}, la 7 {@code [ANIO, LUGARES, PERSONAS]} y la 8 {@code [ANIO, LUGARES]}.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@Import(ModeloDePruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RepreguntaServiceIntegrationTests {

    private static final String USUARIO = "test_historia_rep_a";
    private static final String OTRO = "test_historia_rep_b";
    private static final List<String> USUARIOS = List.of(USUARIO, OTRO);
    private static final Duration ESPERA = Duration.ofSeconds(20);

    private static final int SIN_REPREGUNTAS = 1;
    private static final int CASA = 2;
    private static final int CON_QUIENES_VIVIAS = 3;
    private static final int ESCUELA = 7;
    private static final int MAESTRO = 8;

    @Autowired
    private RepreguntaService repreguntas;
    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private ColaDeExtracciones cola;
    @Autowired
    private EscrituraDelGrafo escritura;
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
    private Persona persona;
    private int minutos;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        recuerdos = new RecuerdosDePrueba(neo4j);
        esperarCola();
        modelo.reiniciar();
        USUARIOS.forEach(personas::borrar);
        persona = personas.crear(USUARIO);
        minutos = 0;
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

    // ---------- Armado del grafo ----------

    /** Un recuerdo sin lugares ni personas, cada uno 1 minuto después del anterior. */
    private String recuerdo(Persona quien, int pregunta, String titulo, Integer anio, String precision, int intentosAnio,
            int intentosLugares, int intentosPersonas) {
        return recuerdos.crear(quien.personaId(), pregunta, titulo, anio, precision, intentosAnio, intentosLugares,
                intentosPersonas, minutos++);
    }

    private String recuerdoSinAnio(int pregunta) {
        return recuerdo(persona, pregunta, "Un recuerdo", null, null, 0, 0, 0);
    }

    private void agregarLugar(String recuerdoId, String nombre) {
        recuerdos.agregarLugar(recuerdoId, nombre);
    }

    private void agregarPersona(Persona quien, String recuerdoId, String nombre, String vinculo) {
        neo4j.query("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona}), (r:Recuerdo {idRecuerdo: $recuerdo})
                CREATE (pe:Persona {idPersona: $id, nombre: $nombre, nombreNormalizado: toLower($nombre)})
                CREATE (p)-[:CONOCE {vinculo: $vinculo}]->(pe)
                CREATE (pe)-[:PARTICIPA_EN]->(r)
                """).bindAll(Map.of("persona", quien.personaId(), "recuerdo", recuerdoId, "id",
                UUID.randomUUID().toString(), "nombre", nombre, "vinculo", vinculo)).run();
    }

    private Map<String, Object> propiedades(String recuerdoId) {
        return recuerdos.propiedades(recuerdoId);
    }

    private long contarRelacion(String recuerdoId, String cypherRelacion) {
        return contar(neo4j, "MATCH (r:Recuerdo {idRecuerdo: $id}) RETURN size([" + cypherRelacion + " | 1]) AS c",
                "id", recuerdoId);
    }

    private long lugaresDe(String recuerdoId) {
        return contarRelacion(recuerdoId, "(r)-[:OCURRIO_EN]->(:Lugar)");
    }

    private long personasDe(String recuerdoId) {
        return contarRelacion(recuerdoId, "(r)<-[:PARTICIPA_EN]-(:Persona)");
    }

    private RepreguntaSiguienteResponse siguiente() {
        return repreguntas.siguiente(persona.cuentaId(), null, null, List.of());
    }

    private RepreguntaSiguienteResponse siguiente(Integer preguntaId, DatoFaltante... excluir) {
        return repreguntas.siguiente(persona.cuentaId(), preguntaId, null, List.of(excluir));
    }

    private void responderYEsperar(String recuerdoId, DatoFaltante dato, String texto) {
        repreguntas.responder(persona.cuentaId(), recuerdoId, dato, texto);
        esperarCola();
    }

    private static String json(String anio, String edad, String personas, String lugares) {
        return "{\"titulo\":\"OTRO TÍTULO\",\"descripcion\":\"OTRA DESCRIPCIÓN\",\"anio\":" + anio
                + ",\"anioAproximado\":false,\"edadMencionada\":" + edad + ",\"personas\":" + personas
                + ",\"lugares\":" + lugares + "}";
    }

    // ---------- 7.5: siguiente ----------

    @Test
    void sinRecuerdosNoHayRepregunta() {
        assertThat(siguiente().estado()).isEqualTo(EstadoRepregunta.NINGUNA);
        assertThat(siguiente().repregunta()).isNull();
    }

    @Test
    void unRecuerdoSinAnioOfreceElAnioConSuTextoFijo() {
        String id = recuerdoSinAnio(ESCUELA);

        RepreguntaSiguienteResponse r = siguiente();

        assertThat(r.estado()).isEqualTo(EstadoRepregunta.HAY);
        assertThat(r.repregunta().recuerdoId()).isEqualTo(id);
        assertThat(r.repregunta().dato()).isEqualTo(ANIO);
        assertThat(r.repregunta().texto()).isEqualTo("¿Te acordás en qué año fue, o cuántos años tenías?");
    }

    @Test
    void losTextosFijosDeLugaresYPersonas() {
        recuerdo(persona, CASA, "Con año", 1958, "EXACTA", 0, 0, 0);
        assertThat(siguiente().repregunta().dato()).isEqualTo(LUGARES);
        assertThat(siguiente().repregunta().texto()).isEqualTo("¿Dónde pasó esto?");

        assertThat(siguiente(null, LUGARES).repregunta().dato()).isEqualTo(PERSONAS);
        assertThat(siguiente(null, LUGARES).repregunta().texto()).isEqualTo("¿Quién más estaba con vos en ese momento? Si te acordás, contame también cómo se llamaban.");
    }

    @Test
    void paraUnaPreguntaSinRepreguntasNuncaHayRepregunta() {
        recuerdoSinAnio(SIN_REPREGUNTAS);

        assertThat(siguiente().estado()).isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @Test
    void soloSeOfrecenDatosDeLaListaDeLaPreguntaDeOrigen() {
        // La 3 solo repregunta por PERSONAS, aunque el recuerdo no tenga año ni lugar.
        recuerdoSinAnio(CON_QUIENES_VIVIAS);

        assertThat(siguiente().repregunta().dato()).isEqualTo(PERSONAS);
        assertThat(siguiente(null, PERSONAS).estado()).isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @ParameterizedTest
    @CsvSource({ "ESTIMADA,HAY", "EXACTA,NINGUNA", "APROXIMADA,NINGUNA", "CALCULADA_POR_EDAD,NINGUNA" })
    void elAnioEsCandidatoConUnaEstimacionYNoConUnAnioReal(String precision, EstadoRepregunta esperado) {
        // La 8 repregunta por ANIO y LUGARES: se le dan lugares para aislar el año.
        String id = recuerdo(persona, MAESTRO, "Un recuerdo", 1957, precision, 0, 0, 0);
        agregarLugar(id, "Allen");

        assertThat(siguiente().estado()).isEqualTo(esperado);
        if (esperado == EstadoRepregunta.HAY) {
            assertThat(siguiente().repregunta().dato()).isEqualTo(ANIO);
        }
    }

    @Test
    void lugaresYPersonasSoloSiElRecuerdoNoTieneNinguno() {
        String conLugar = recuerdo(persona, CASA, "Con lugar", 1958, "EXACTA", 0, 0, 0);
        agregarLugar(conLugar, "Allen");
        String conPersona = recuerdo(persona, CASA, "Con persona", 1958, "EXACTA", 0, 0, 0);
        agregarPersona(persona, conPersona, "Marta", "hermana");

        // El primero tiene lugar pero no personas; el segundo, persona pero no lugares.
        RepreguntaSiguienteResponse primero = siguiente();
        assertThat(primero.repregunta().recuerdoId()).isEqualTo(conLugar);
        assertThat(primero.repregunta().dato()).isEqualTo(PERSONAS);

        agregarPersona(persona, conLugar, "Rosa", "madre");
        RepreguntaSiguienteResponse segundo = siguiente();
        assertThat(segundo.repregunta().recuerdoId()).isEqualTo(conPersona);
        assertThat(segundo.repregunta().dato()).isEqualTo(LUGARES);

        agregarLugar(conPersona, "Cipolletti");
        assertThat(siguiente().estado()).isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @Test
    void excluirSacaEsosDatosDelResultado() {
        recuerdoSinAnio(ESCUELA);

        assertThat(siguiente().repregunta().dato()).isEqualTo(ANIO);
        assertThat(siguiente(null, ANIO).repregunta().dato()).isEqualTo(LUGARES);
        assertThat(siguiente(null, ANIO, LUGARES).repregunta().dato()).isEqualTo(PERSONAS);
        assertThat(siguiente(null, ANIO, LUGARES, PERSONAS).estado()).isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @Test
    void conElContadorEnElMaximoYaNoSeOfrece() {
        String id = recuerdo(persona, ESCUELA, "Un recuerdo", null, null, 2, 1, 0);

        RepreguntaSiguienteResponse r = siguiente();

        assertThat(r.repregunta().recuerdoId()).isEqualTo(id);
        assertThat(r.repregunta().dato()).as("ANIO ya se preguntó 2 veces").isEqualTo(LUGARES);

        recuerdo(persona, ESCUELA, "Otro", 1957, "EXACTA", 0, 2, 2);
        assertThat(siguiente(null, LUGARES).repregunta().dato()).isEqualTo(PERSONAS);
    }

    @Test
    void sinPreguntaIdDevuelveElRecuerdoMasAntiguoConUnaCandidata() {
        String sinCandidata = recuerdo(persona, SIN_REPREGUNTAS, "Sin repreguntas", null, null, 0, 0, 0);
        String antiguo = recuerdoSinAnio(ESCUELA);
        String nuevo = recuerdoSinAnio(ESCUELA);

        assertThat(sinCandidata).isNotEqualTo(antiguo);
        assertThat(siguiente().repregunta().recuerdoId()).isEqualTo(antiguo);
        // Con el primero descartado por excluir, sigue siendo el más antiguo (otro dato), no el nuevo.
        assertThat(siguiente(null, ANIO).repregunta().recuerdoId()).isEqualTo(antiguo);
        assertThat(nuevo).isNotEqualTo(antiguo);
    }

    @Test
    void conPreguntaIdBuscaSoloEnElRecuerdoQueSurgioDeEsaPregunta() {
        recuerdoSinAnio(ESCUELA);
        String deLaCasa = recuerdoSinAnio(CASA);

        RepreguntaSiguienteResponse r = siguiente(CASA);

        assertThat(r.repregunta().recuerdoId()).isEqualTo(deLaCasa);
        assertThat(r.repregunta().dato()).isEqualTo(LUGARES);
    }

    @Test
    void unPreguntaIdDesconocidoOSinRecuerdosDaNingunaYNoUn404() {
        recuerdoSinAnio(ESCUELA);

        assertThat(siguiente(9999).estado()).isEqualTo(EstadoRepregunta.NINGUNA);
        assertThat(siguiente(MAESTRO).estado()).as("hay recuerdos, pero no de esa pregunta")
                .isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @Test
    void losRecuerdosDeOtraPersonaNoSeOfrecen() {
        Persona otra = personas.crear(OTRO);
        recuerdo(otra, ESCUELA, "De la otra persona", null, null, 0, 0, 0);

        assertThat(siguiente().estado()).isEqualTo(EstadoRepregunta.NINGUNA);
    }

    @Test
    void mientrasLaExtraccionEstaEnCursoDicePROCESANDOYDespuesLaRepregunta() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.respondeConDemora(json("1962", "null", "[]", "[]"), 500);

        repreguntas.responder(persona.cuentaId(), id, ANIO, "Fue en 1962");
        RepreguntaSiguienteResponse durante = siguiente();
        esperarCola();

        assertThat(durante.estado()).isEqualTo(EstadoRepregunta.PROCESANDO);
        assertThat(durante.repregunta()).isNull();
        // El año ya quedó fijado: ahora el dato que sigue es LUGARES.
        assertThat(siguiente().repregunta().dato()).isEqualTo(LUGARES);
    }

    @Test
    void sinPersonaMayorSiguienteDa404() {
        Long cuentaSinPersona = personas.crearCuentaSinPersona("test_historia_rep_sin_persona");
        try {
            assertThatThrownBy(() -> repreguntas.siguiente(cuentaSinPersona, null, null, List.of()))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        } finally {
            personas.borrar("test_historia_rep_sin_persona");
        }
    }

    // ---------- 7.6: responder ----------

    @Test
    void responderSumaUnoAlContadorDeEseDatoYNoDeLosOtros() {
        String id = recuerdoSinAnio(ESCUELA);

        responderYEsperar(id, LUGARES, "En Allen");

        assertThat(propiedades(id)).containsEntry("intentosLugares", 1L).containsEntry("intentosAnio", 0L)
                .containsEntry("intentosPersonas", 0L);
    }

    @Test
    void responderAnioReemplazaUnaEstimacionYFijaPrecisionYEtapa() {
        String id = recuerdo(persona, ESCUELA, "Un recuerdo", 1959, "ESTIMADA", 0, 0, 0);
        modelo.responde(json("1962", "null", "[]", "[]"));

        responderYEsperar(id, ANIO, "Fue en 1962");

        // 1962 - 1950 = 12 años: ADOLESCENCIA (el límite 11/12 de la etapa de vida).
        assertThat(propiedades(id)).containsEntry("anio", 1962L).containsEntry("precision", "EXACTA")
                .containsEntry("etapa", "ADOLESCENCIA").containsEntry("intentosAnio", 1L);
    }

    @Test
    void responderAnioConUnaEdadCalculaElAnioPorEdad() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.responde(json("null", "30", "[]", "[]"));

        responderYEsperar(id, ANIO, "Y fue cuando yo tenía 30 años.");

        // 1950 + 30 = 1980, a los 30: ADULTEZ.
        assertThat(propiedades(id)).containsEntry("anio", 1980L).containsEntry("precision", "CALCULADA_POR_EDAD")
                .containsEntry("etapa", "ADULTEZ");
    }

    @Test
    void unAnioQueNoEstaEscritoEnLaRespuestaNoSeAplica() {
        String id = recuerdoSinAnio(ESCUELA);
        // El modelo "inventa" 1962 pero la persona no lo escribió: la validación de contenido lo descarta.
        modelo.responde(json("1962", "null", "[]", "[]"));

        responderYEsperar(id, ANIO, "Fue cuando era chica");

        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("intentosAnio", 1L);
    }

    @Test
    void unAnioExactoQueAparecioMientrasTantoNoSePisa() {
        String id = recuerdoSinAnio(ESCUELA);
        // La extracción tarda; mientras tanto otro proceso fija un año exacto. La respuesta no lo pisa.
        modelo.respondeConDemora(json("1962", "null", "[]", "[]"), 600);

        repreguntas.responder(persona.cuentaId(), id, ANIO, "Fue en 1962");
        neo4j.query("MATCH (r:Recuerdo {idRecuerdo: $id}) SET r.anio = 1955, r.precision = 'EXACTA', r.etapaDeVida = 'INFANCIA'")
                .bind(id).to("id").run();
        esperarCola();

        assertThat(propiedades(id)).containsEntry("anio", 1955L).containsEntry("precision", "EXACTA");
    }

    @Test
    void responderLugaresSumaSinDuplicarUnLugarQueYaExisteEnOtroRecuerdo() {
        String otro = recuerdo(persona, CASA, "Otro", 1958, "EXACTA", 0, 0, 0);
        agregarLugar(otro, "Allen");
        String id = recuerdo(persona, ESCUELA, "Un recuerdo", 1957, "EXACTA", 0, 0, 0);
        modelo.responde(json("null", "null", "[]", "[\"Allen\",\"ALLEN\",\"Cipolletti\"]"));

        responderYEsperar(id, LUGARES, "En Allen, cerca de Cipolletti");

        assertThat(lugaresDe(id)).isEqualTo(2);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(l:Lugar) RETURN count(DISTINCT l) AS c",
                "p", persona.personaId())).as("Allen se reutilizó: dos lugares en total").isEqualTo(2);
    }

    @Test
    void responderPersonasSumaSinDuplicarYReutilizaALaQueYaExiste() {
        String otro = recuerdo(persona, CASA, "Otro", 1958, "EXACTA", 0, 0, 0);
        agregarPersona(persona, otro, "Marta", "hermana");
        String id = recuerdo(persona, ESCUELA, "Un recuerdo", 1957, "EXACTA", 0, 0, 0);
        modelo.responde(json("null", "null", "[{\"nombre\":\"MARTA\",\"vinculo\":\"prima\"},{\"nombre\":null,\"vinculo\":\"tío\"}]", "[]"));

        responderYEsperar(id, PERSONAS, "Con mi hermana Marta y mi tío");

        assertThat(personasDe(id)).isEqualTo(2);
        List<Map<String, Object>> conocidas = filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $p})-[c:CONOCE]->(pe:Persona)
                RETURN pe.nombre AS nombre, c.vinculo AS vinculo ORDER BY coalesce(pe.nombre, '')
                """, "p", persona.personaId());
        assertThat(conocidas).hasSize(2);
        assertThat(conocidas.get(1)).as("Marta sigue con su vínculo de siempre").containsEntry("vinculo", "hermana");
    }

    @Test
    void soloSeAplicaLoDelDatoPreguntado() {
        String id = recuerdoSinAnio(ESCUELA);
        // El modelo devuelve de todo; al preguntar por el año solo cuenta el año.
        modelo.responde(json("1962", "null", "[{\"nombre\":\"Elena\",\"vinculo\":\"maestra\"}]", "[\"Allen\"]"));

        responderYEsperar(id, ANIO, "Fue en 1962, con Elena, en Allen");

        assertThat(propiedades(id)).containsEntry("anio", 1962L);
        assertThat(lugaresDe(id)).isZero();
        assertThat(personasDe(id)).isZero();
    }

    @Test
    void nuncaCambiaElTituloNiLaDescripcion() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.responde(json("1962", "null", "[]", "[]"));

        responderYEsperar(id, ANIO, "Fue en 1962");

        assertThat(propiedades(id)).containsEntry("titulo", "Un recuerdo").containsEntry("descripcion", "Descripción original.");
    }

    @Test
    void conNoMeAcuerdoElRecuerdoNoCambiaPeroElIntentoCuenta() {
        String id = recuerdoSinAnio(ESCUELA);
        // El modelo no encuentra nada: el JSON genérico no tiene año, lugares ni personas.

        responderYEsperar(id, ANIO, "No me acuerdo.");

        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("intentosAnio", 1L);
        assertThat(lugaresDe(id)).isZero();
        assertThat(personasDe(id)).isZero();
    }

    @Test
    void noMeAcuerdoNoLeHaceNingunPedidoAlModeloPeroElIntentoCuenta() {
        String id = recuerdoSinAnio(ESCUELA);

        repreguntas.responder(persona.cuentaId(), id, ANIO, "No me acuerdo.");

        assertThat(cola.estaProcesando(persona.personaId())).as("no hay nada en la cola").isFalse();
        esperarCola();
        assertThat(modelo.pedidos()).as("no se consultó al modelo").isEmpty();
        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("intentosAnio", 1L);
        // Igual que antes: el dato se vuelve a ofrecer hasta agotar los 2 intentos.
        assertThat(siguiente().repregunta().dato()).isEqualTo(ANIO);
    }

    @Test
    void noQuieroContarOResponderTampocoConsultaAlModeloYCuentaElIntento() {
        String id = recuerdoSinAnio(ESCUELA);

        repreguntas.responder(persona.cuentaId(), id, LUGARES, "No quiero contar");
        repreguntas.responder(persona.cuentaId(), id, PERSONAS, "No quiero responder, perdón");

        esperarCola();
        assertThat(modelo.pedidos()).isEmpty();
        assertThat(propiedades(id)).containsEntry("intentosLugares", 1L).containsEntry("intentosPersonas", 1L)
                .containsEntry("intentosAnio", 0L);
    }

    @Test
    void conNoMeAcuerdoDosVecesElDatoSeAgotaYUnTerceroDa409() {
        String id = recuerdoSinAnio(ESCUELA);

        repreguntas.responder(persona.cuentaId(), id, ANIO, "No sé");
        repreguntas.responder(persona.cuentaId(), id, ANIO, "Ni idea");

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), id, ANIO, "No sé"))
                .isInstanceOf(ConflictoException.class);
        assertThat(propiedades(id)).containsEntry("intentosAnio", 2L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void unaRepreguntaQueMezclaNoMeAcuerdoConContenidoSiConsultaAlModelo() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.responde(json("1962", "null", "[]", "[]"));

        responderYEsperar(id, ANIO, "No me acuerdo del día, pero fue en 1962");

        assertThat(modelo.pedidos()).hasSize(1);
        assertThat(propiedades(id)).containsEntry("anio", 1962L).containsEntry("intentosAnio", 1L);
    }

    @Test
    void conRiesgoUnNoMeAcuerdoNoCuentaElIntentoNiConsultaAlModelo() {
        String id = recuerdoSinAnio(ESCUELA);

        var respuesta = repreguntas.responder(persona.cuentaId(), id, ANIO, "No me acuerdo, ya no quiero vivir");

        assertThat(respuesta.estado().name()).isEqualTo("ALARMA");
        assertThat(propiedades(id)).containsEntry("intentosAnio", 0L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void siElModeloNoEstaDisponibleElRecuerdoNoCambiaYElIntentoSeDevuelve() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.falla();

        responderYEsperar(id, ANIO, "Fue en 1962");

        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("titulo", "Un recuerdo")
                .containsEntry("intentosAnio", 0L);
        assertThat(contar(neo4j, "MATCH (r:Recuerdo) WHERE (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                "p", persona.personaId())).as("una repregunta no tiene respaldo: no se crea otro recuerdo").isEqualTo(1);
        assertThat(siguiente().repregunta().dato()).as("el dato se vuelve a ofrecer").isEqualTo(ANIO);
    }

    @Test
    void conDosErroresTransitoriosSeReintentaUnaVezYElIntentoSeDevuelve() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.fallaTransitoria().fallaTransitoria();

        responderYEsperar(id, ANIO, "Fue en 1962");

        assertThat(modelo.pedidos()).as("un pedido y un reintento").hasSize(2);
        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("intentosAnio", 0L);
    }

    @Test
    void unErrorTransitorioSeReintentaYSiElSegundoPedidoSirveSeCompletaElRecuerdoYElIntentoCuenta() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.fallaTransitoria().responde(json("1962", "null", "[]", "[]"));

        responderYEsperar(id, ANIO, "Fue en 1962");

        assertThat(modelo.pedidos()).hasSize(2);
        assertThat(propiedades(id)).containsEntry("anio", 1962L).containsEntry("intentosAnio", 1L);
    }

    @Test
    void siElModeloRespondeJsonInvalidoDosVecesElRecuerdoNoCambiaYElIntentoSigueContado() {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.responde("no es json").responde("{ tampoco");

        responderYEsperar(id, ANIO, "Fue en 1962");

        assertThat(propiedades(id)).containsEntry("anio", null).containsEntry("intentosAnio", 1L);
    }

    @Test
    void devolverUnIntentoNuncaDejaElContadorPorDebajoDeCero() {
        String id = recuerdoSinAnio(ESCUELA);

        escritura.devolverIntento(persona.personaId(), id, ANIO);
        escritura.devolverIntento(persona.personaId(), id, ANIO);

        assertThat(propiedades(id)).containsEntry("intentosAnio", 0L);
    }

    @Test
    void devolverUnIntentoBajaSoloElContadorDeEseDatoDeEseRecuerdo() {
        String id = recuerdo(persona, ESCUELA, "Uno", null, null, 2, 1, 1);
        String otro = recuerdo(persona, ESCUELA, "Otro", null, null, 2, 1, 1);

        escritura.devolverIntento(persona.personaId(), id, ANIO);

        assertThat(propiedades(id)).containsEntry("intentosAnio", 1L).containsEntry("intentosLugares", 1L)
                .containsEntry("intentosPersonas", 1L);
        assertThat(propiedades(otro)).containsEntry("intentosAnio", 2L);
    }

    @Test
    void devolverUnIntentoNoTocaUnRecuerdoDeOtraPersona() {
        Persona otra = personas.crear(OTRO);
        String ajeno = recuerdo(otra, ESCUELA, "Ajeno", null, null, 2, 0, 0);

        escritura.devolverIntento(persona.personaId(), ajeno, ANIO);

        assertThat(propiedades(ajeno)).containsEntry("intentosAnio", 2L);
    }

    @Test
    void elExtractorRecibeElTextoFijoDeLaRepreguntaYNoElTituloDelRecuerdo() {
        String id = recuerdo(persona, ESCUELA, "TITULO_DEL_RECUERDO", null, null, 0, 0, 0);

        responderYEsperar(id, LUGARES, "En Allen");

        assertThat(modelo.pedidos()).hasSize(1);
        String mensaje = modelo.pedidos().get(0).mensaje();
        assertThat(mensaje).contains("Pregunta: ¿Dónde pasó esto?").contains("Respuesta: En Allen")
                .doesNotContain("TITULO_DEL_RECUERDO");
    }

    @Test
    void nuncaCreaPreguntasNiTocaElProgreso() {
        String id = recuerdoSinAnio(ESCUELA);
        historia.saltear(persona.cuentaId(), 4);
        long preguntasAntes = contar(neo4j, "MATCH (q:PreguntaGuion) RETURN count(q) AS c");
        long progresoAntes = contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->() RETURN count(r) AS c",
                "p", persona.personaId());

        responderYEsperar(id, ANIO, "Fue en 1962");
        repreguntas.saltear(persona.cuentaId(), id, LUGARES);

        assertThat(contar(neo4j, "MATCH (q:PreguntaGuion) RETURN count(q) AS c")).isEqualTo(preguntasAntes);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->() RETURN count(r) AS c",
                "p", persona.personaId())).isEqualTo(progresoAntes);
        assertThat(propiedades(id)).containsEntry("intentosAnio", 1L).containsEntry("intentosLugares", 1L);
    }

    // ---------- Errores de 7.6 y 7.7 ----------

    @Test
    void conElContadorEnElMaximoResponderYSaltearDan409() {
        String id = recuerdo(persona, ESCUELA, "Un recuerdo", null, null, 2, 0, 0);

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), id, ANIO, "Fue en 1962"))
                .isInstanceOf(ConflictoException.class).hasMessage("Esa pregunta ya no hace falta.");
        assertThatThrownBy(() -> repreguntas.saltear(persona.cuentaId(), id, ANIO))
                .isInstanceOf(ConflictoException.class).hasMessage("Esa pregunta ya no hace falta.");
        assertThat(modelo.pedidos()).as("un 409 no encola nada").isEmpty();
        assertThat(propiedades(id)).containsEntry("intentosAnio", 2L);
    }

    @Test
    void unDatoQueNoEstaEnLaListaDeLaPreguntaDeOrigenDa409() {
        String sinLista = recuerdoSinAnio(SIN_REPREGUNTAS);
        String soloAnioYLugares = recuerdoSinAnio(MAESTRO);

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), sinLista, ANIO, "Fue en 1962"))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), soloAnioYLugares, PERSONAS, "Con Marta"))
                .isInstanceOf(ConflictoException.class);
        assertThat(propiedades(sinLista)).containsEntry("intentosAnio", 0L);
    }

    @Test
    void unDatoQueYaNoFaltaDa409() {
        String conAnioReal = recuerdo(persona, ESCUELA, "Un recuerdo", 1957, "EXACTA", 0, 0, 0);
        String conLugar = recuerdo(persona, ESCUELA, "Otro", 1957, "EXACTA", 0, 0, 0);
        agregarLugar(conLugar, "Allen");

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), conAnioReal, ANIO, "Fue en 1957"))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> repreguntas.saltear(persona.cuentaId(), conLugar, LUGARES))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void unRecuerdoInexistenteODeOtraPersonaDa404() {
        Persona otra = personas.crear(OTRO);
        String deLaOtra = recuerdo(otra, ESCUELA, "De la otra", null, null, 0, 0, 0);

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), UUID.randomUUID().toString(), ANIO, "Fue en 1962"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), deLaOtra, ANIO, "Fue en 1962"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> repreguntas.saltear(persona.cuentaId(), deLaOtra, ANIO))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(propiedades(deLaOtra)).as("el contador de la otra persona no se toca").containsEntry("intentosAnio", 0L);
    }

    @Test
    void unaRespuestaMuyLargaDa400YNoTocaElGrafo() {
        String id = recuerdoSinAnio(ESCUELA);

        assertThatThrownBy(() -> repreguntas.responder(persona.cuentaId(), id, ANIO, "x".repeat(1001)))
                .isInstanceOf(DatosInvalidosException.class);

        assertThat(propiedades(id)).containsEntry("intentosAnio", 0L);
        assertThat(modelo.pedidos()).isEmpty();
    }

    // ---------- 7.7: saltear ----------

    @Test
    void saltearSumaUnoNoEncolaNadaYNoTocaElRecuerdo() {
        String id = recuerdoSinAnio(ESCUELA);

        repreguntas.saltear(persona.cuentaId(), id, ANIO);
        esperarCola();

        assertThat(propiedades(id)).containsEntry("intentosAnio", 1L).containsEntry("anio", null)
                .containsEntry("titulo", "Un recuerdo");
        assertThat(modelo.pedidos()).isEmpty();
    }

    @Test
    void responderYSaltearCuentanIgualYALosDosIntentosYaNoHaceFalta() {
        String id = recuerdoSinAnio(ESCUELA);

        repreguntas.saltear(persona.cuentaId(), id, ANIO);
        responderYEsperar(id, ANIO, "No me acuerdo.");

        assertThat(propiedades(id)).containsEntry("intentosAnio", 2L);
        assertThatThrownBy(() -> repreguntas.saltear(persona.cuentaId(), id, ANIO)).isInstanceOf(ConflictoException.class);
        assertThat(siguiente().repregunta().dato()).as("ANIO ya no se ofrece").isEqualTo(LUGARES);
    }

    // ---------- Concurrencia ----------

    @Test
    void dosPedidosALaVezConUnIntentoYaUsadoDejanExactamenteUnoRegistrado() throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int ronda = 1; ronda <= 15; ronda++) {
                String id = recuerdo(persona, ESCUELA, "Ronda " + ronda, null, null, 1, 0, 0);
                CyclicBarrier barrera = new CyclicBarrier(2);
                List<Future<Boolean>> pedidos = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    boolean responder = i == 0;
                    pedidos.add(hilos.submit(() -> {
                        barrera.await(5, TimeUnit.SECONDS);
                        try {
                            if (responder) {
                                repreguntas.responder(persona.cuentaId(), id, ANIO, "No me acuerdo.");
                            } else {
                                repreguntas.saltear(persona.cuentaId(), id, ANIO);
                            }
                            return true;
                        } catch (ConflictoException e) {
                            return false;
                        }
                    }));
                }
                long registrados = 0;
                for (Future<Boolean> pedido : pedidos) {
                    registrados += pedido.get(10, TimeUnit.SECONDS) ? 1 : 0;
                }
                assertThat(registrados).as("ronda " + ronda + ": uno registrado y el otro 409").isEqualTo(1);
                assertThat(propiedades(id)).as("ronda " + ronda).containsEntry("intentosAnio", 2L);
            }
        } finally {
            hilos.shutdownNow();
            esperarCola();
        }
    }

    // ---------- Privacidad (5.9) ----------

    @Test
    void ningunLogContieneElTextoDeLaRespuestaNiLosNombres(CapturedOutput salida) {
        String id = recuerdoSinAnio(ESCUELA);
        modelo.responde(json("null", "null", "[{\"nombre\":\"NOMBRE_SECRETO\",\"vinculo\":\"amiga\"}]", "[]"));
        responderYEsperar(id, PERSONAS, "TEXTO_SECRETO con NOMBRE_SECRETO");
        modelo.falla();
        responderYEsperar(id, LUGARES, "OTRO_TEXTO_SECRETO");

        assertThat(salida.getAll()).doesNotContain("TEXTO_SECRETO").doesNotContain("NOMBRE_SECRETO");
    }
}
