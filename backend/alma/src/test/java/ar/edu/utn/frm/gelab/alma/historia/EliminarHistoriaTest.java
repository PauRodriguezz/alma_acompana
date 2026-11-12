package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.PERSONA_A;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.PERSONA_B;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;

import ar.edu.utn.frm.gelab.alma.historia.config.CargaInicialDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.PreguntaGuionRepository;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;

/**
 * {@code eliminarHistoria} (contrato del grafo, 5.10 y sección 11, "Baja"). Mismas condiciones que
 * {@link CargaInicialDelGrafoTest}: seed apagado, personas de prueba con ids negativos y limpieza antes y
 * después de cada prueba.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
class EliminarHistoriaTest {

    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private CargaInicialDelGrafo carga;
    @Autowired
    private PreguntaGuionRepository preguntaRepository;
    @Autowired
    private Neo4jClient neo4j;

    @BeforeEach
    @AfterEach
    void limpiar() {
        historia.eliminarHistoria(PERSONA_A);
        historia.eliminarHistoria(PERSONA_B);
    }

    @Test
    void borraElNodoYTodosSusRecuerdosPersonasYLugares() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);
        List<String> recuerdos = ids("MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(n:Recuerdo) RETURN n.idRecuerdo AS id", PERSONA_A);
        List<String> personas = ids("MATCH (:PersonaMayor {idPersonaMayor: $p})-[:CONOCE]->(n:Persona) RETURN n.idPersona AS id", PERSONA_A);
        List<String> lugares = ids("MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(n:Lugar) "
                + "RETURN DISTINCT n.idLugar AS id", PERSONA_A);
        assertThat(recuerdos).hasSize(4);
        assertThat(personas).hasSize(4);
        assertThat(lugares).hasSize(3);

        historia.eliminarHistoria(PERSONA_A);

        assertThat(contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p", PERSONA_A)).isZero();
        assertThat(contar(neo4j, "MATCH (n:Recuerdo) WHERE n.idRecuerdo IN $ids RETURN count(n) AS c", "ids", recuerdos)).isZero();
        assertThat(contar(neo4j, "MATCH (n:Persona) WHERE n.idPersona IN $ids RETURN count(n) AS c", "ids", personas)).isZero();
        assertThat(contar(neo4j, "MATCH (n:Lugar) WHERE n.idLugar IN $ids RETURN count(n) AS c", "ids", lugares)).isZero();
    }

    @Test
    void noTocaLasPreguntasDelGuionNiLaHistoriaDeOtraPersona() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);
        carga.cargarHistoriaDeEjemplo(PERSONA_B);
        long preguntasAntes = preguntaRepository.count();

        historia.eliminarHistoria(PERSONA_A);

        assertThat(preguntaRepository.count()).isEqualTo(preguntasAntes);
        // Siguen las relaciones de la otra persona con las preguntas, y todo lo suyo.
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->() RETURN count(r) AS c",
                "p", PERSONA_B)).isEqualTo(5);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                "p", PERSONA_B)).isEqualTo(4);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:CONOCE]->(pe) RETURN count(pe) AS c",
                "p", PERSONA_B)).isEqualTo(4);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(l) "
                + "RETURN count(DISTINCT l) AS c", "p", PERSONA_B)).isEqualTo(3);
    }

    @Test
    void repetirlaNoDaErrorYUnaPersonaSinNodoTampoco() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);

        assertThatCode(() -> {
            historia.eliminarHistoria(PERSONA_A);
            historia.eliminarHistoria(PERSONA_A);
            historia.eliminarHistoria(PERSONA_B);
        }).doesNotThrowAnyException();
    }

    @Test
    void unaPersonaSoloConProgresoTambienSeBorra() {
        neo4j.query("""
                MERGE (p:PersonaMayor {idPersonaMayor: $p})
                WITH p MATCH (q:PreguntaGuion {idPreguntaGuion: 1})
                CREATE (p)-[:RESPONDIO {fecha: datetime()}]->(q)
                """).bind(PERSONA_A).to("p").run();

        historia.eliminarHistoria(PERSONA_A);

        assertThat(contar(neo4j, "MATCH (p:PersonaMayor {idPersonaMayor: $p}) RETURN count(p) AS c", "p", PERSONA_A)).isZero();
        assertThat(preguntaRepository.findById(1)).isPresent();
    }

    private List<String> ids(String cypher, long persona) {
        List<Map<String, Object>> filas = filas(neo4j, cypher, "p", persona);
        return filas.stream().map(fila -> (String) fila.get("id")).toList();
    }
}
