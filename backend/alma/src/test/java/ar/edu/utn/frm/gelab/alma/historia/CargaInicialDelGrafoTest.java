package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.PERSONA_A;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.PERSONA_B;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;

import ar.edu.utn.frm.gelab.alma.historia.config.CargaInicialDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaGuion;
import ar.edu.utn.frm.gelab.alma.historia.repository.PreguntaGuionRepository;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;

/**
 * Carga inicial del grafo (contrato del grafo, sección 11, "Grafo y guion"), contra el Neo4j de
 * {@code docker compose up -d}. Con {@code alma.seed.enabled=false}: la carga de {@code ana} no corre y
 * estas pruebas nunca tocan su historia. Las personas de prueba tienen ids negativos y las preguntas de
 * prueba ids desde 9001; todo se borra antes y después de cada prueba (también si una falla a la mitad).
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
class CargaInicialDelGrafoTest {

    private static final Set<String> NOMBRES_DE_RESTRICCIONES = Set.of("personaMayor_id", "preguntaGuion_id",
            "preguntaGuion_orden", "recuerdo_id", "persona_id", "lugar_id");

    @Autowired
    private CargaInicialDelGrafo carga;
    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private PreguntaGuionRepository preguntaRepository;
    @Autowired
    private Neo4jClient neo4j;

    @BeforeEach
    @AfterEach
    void limpiar() {
        historia.eliminarHistoria(PERSONA_A);
        historia.eliminarHistoria(PERSONA_B);
        GrafoDePrueba.borrarPreguntasDePrueba(neo4j);
    }

    // ---------- Restricciones y guion ----------

    @Test
    void existenLasSeisRestricciones() {
        Set<String> existentes = filas(neo4j, "SHOW CONSTRAINTS YIELD name RETURN name").stream()
                .map(fila -> (String) fila.get("name")).collect(Collectors.toSet());

        assertThat(existentes).containsAll(NOMBRES_DE_RESTRICCIONES);
    }

    @Test
    void alArrancarHayVeintiunaPreguntasConSusRepreguntas() {
        List<PreguntaDelGuion> esperadas = carga.leerGuion();
        List<PreguntaGuion> enElGrafo = preguntaRepository.findAllByOrderByOrdenAsc();

        assertThat(enElGrafo).hasSize(21);
        for (PreguntaDelGuion esperada : esperadas) {
            PreguntaGuion pregunta = enElGrafo.stream().filter(p -> p.getIdPreguntaGuion() == esperada.id())
                    .findFirst().orElseThrow();
            assertThat(pregunta.getOrden()).isEqualTo(esperada.orden());
            assertThat(pregunta.getTexto()).isNotBlank();
            assertThat(pregunta.getEdadDesde()).isEqualTo(esperada.edadDesde());
            assertThat(pregunta.getEdadHasta()).isEqualTo(esperada.edadHasta());
            assertThat(pregunta.getRepreguntas()).as("repreguntas de la pregunta %d", esperada.id())
                    .containsExactlyElementsOf(esperada.repreguntas());
        }
    }

    @Test
    void arrancarDosVecesNoDuplicaNada() {
        carga.cargar();
        carga.cargar();

        assertThat(preguntaRepository.findAllByOrderByOrdenAsc()).hasSize(21);
        assertThat(carga.cargarGuion(carga.leerGuion())).isZero();
        assertThat(filas(neo4j, "SHOW CONSTRAINTS YIELD name RETURN name").stream()
                .map(fila -> (String) fila.get("name")).filter(NOMBRES_DE_RESTRICCIONES::contains)).hasSize(6);
    }

    @Test
    void laCargaNoPisaCambiosYSiCreaLaPreguntaQueFalta() {
        PreguntaDelGuion primera = new PreguntaDelGuion(9001, 9001, "Texto original", null, null, List.of("ANIO"));
        PreguntaDelGuion segunda = new PreguntaDelGuion(9002, 9002, "Pregunta nueva", 6, 12, List.of());

        assertThat(carga.cargarGuion(List.of(primera))).isEqualTo(1);
        neo4j.query("MATCH (q:PreguntaGuion {idPreguntaGuion: 9001}) SET q.texto = 'Texto editado a mano'").run();

        // Se vuelve a ejecutar la carga con el texto original y con una pregunta que no existe.
        assertThat(carga.cargarGuion(List.of(primera, segunda))).isEqualTo(1);

        PreguntaGuion existente = preguntaRepository.findById(9001).orElseThrow();
        assertThat(existente.getTexto()).isEqualTo("Texto editado a mano");
        assertThat(existente.getRepreguntas()).containsExactly("ANIO");
        PreguntaGuion nueva = preguntaRepository.findById(9002).orElseThrow();
        assertThat(nueva.getTexto()).isEqualTo("Pregunta nueva");
        assertThat(nueva.getEdadDesde()).isEqualTo(6);
        assertThat(nueva.getEdadHasta()).isEqualTo(12);
        assertThat(nueva.getRepreguntas()).isEmpty();
    }

    @Test
    void unaPreguntaConOtroIdPeroElMismoOrdenNoSeCargaYNoRompeLoQueYaEstaba() {
        // La restricción de unicidad de "orden" rechaza el choque: la transacción se deshace entera.
        PreguntaDelGuion choca = new PreguntaDelGuion(9001, 1, "Choca con el orden 1", null, null, List.of());
        PreguntaDelGuion valida = new PreguntaDelGuion(9002, 9002, "Esta no debería quedar", null, null, List.of());

        assertThatThrownBy(() -> carga.cargarGuion(List.of(valida, choca))).isInstanceOf(RuntimeException.class);

        assertThat(preguntaRepository.findById(9001)).isEmpty();
        assertThat(preguntaRepository.findById(9002)).isEmpty();
        assertThat(preguntaRepository.findAllByOrderByOrdenAsc()).hasSize(21);
    }

    // ---------- Historia de ejemplo (Anexo B) ----------

    @Test
    void laHistoriaDeEjemploDejaElProgresoDelAnexoB() {
        assertThat(carga.cargarHistoriaDeEjemplo(PERSONA_A)).isTrue();

        assertThat(preguntas("RESPONDIO", PERSONA_A)).containsExactlyInAnyOrder(1, 2, 7, 17);
        assertThat(preguntas("SALTEO", PERSONA_A)).containsExactly(8);
        // La próxima pendiente es la 3 (la de menor orden sin respuesta ni salteo).
        assertThat(GrafoDePrueba.proximaPregunta(neo4j, PERSONA_A)).isEqualTo(3);
    }

    @Test
    void laHistoriaDeEjemploCreaLosCuatroRecuerdosDelAnexoB() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);

        List<Map<String, Object>> recuerdos = filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
                RETURN r.titulo AS titulo, q.idPreguntaGuion AS pregunta, r.anio AS anio, r.precision AS precision,
                       r.etapaDeVida AS etapa, r.intentosAnio AS intentosAnio, r.intentosLugares AS intentosLugares,
                       r.intentosPersonas AS intentosPersonas, r.descripcion AS descripcion, r.fechaRegistro AS registro
                ORDER BY q.orden
                """, "persona", PERSONA_A);

        assertThat(recuerdos).hasSize(4);
        assertRecuerdo(recuerdos.get(0), "Nací en Cipolletti", 1, 1950L, "ESTIMADA", "INFANCIA", 0L);
        assertRecuerdo(recuerdos.get(1), "La casa de la calle San Martín", 2, 1958L, "APROXIMADA", "INFANCIA", 0L);
        assertRecuerdo(recuerdos.get(2), "Mi maestra Elena", 7, 1957L, "EXACTA", "INFANCIA", 0L);
        // Sin año, sin precisión y sin etapa (regla 6), y con un intento de año ya usado.
        assertRecuerdo(recuerdos.get(3), "Un viaje a la costa", 17, null, null, null, 1L);
        assertThat(recuerdos).allSatisfy(r -> {
            assertThat((String) r.get("descripcion")).isNotBlank();
            assertThat(r.get("registro")).isNotNull();
            assertThat(r.get("intentosLugares")).isEqualTo(0L);
            assertThat(r.get("intentosPersonas")).isEqualTo(0L);
        });
    }

    @Test
    void laHistoriaDeEjemploCreaLasPersonasYLosLugaresDelAnexoB() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);

        List<Map<String, Object>> personas = filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[c:CONOCE]->(pe:Persona)-[:PARTICIPA_EN]->(r:Recuerdo)
                RETURN pe.nombre AS nombre, pe.nombreNormalizado AS normalizado, c.vinculo AS vinculo, r.titulo AS recuerdo
                """, "persona", PERSONA_A);
        assertThat(personas).extracting(p -> p.get("nombre") + "|" + p.get("vinculo") + "|" + p.get("recuerdo"))
                .containsExactlyInAnyOrder("Marta|hermana|La casa de la calle San Martín",
                        "Rosa|madre|La casa de la calle San Martín", "Elena|maestra|Mi maestra Elena",
                        "null|marido|Un viaje a la costa");
        // La persona sin nombre no tiene ni nombre ni nombre normalizado.
        assertThat(personas.stream().filter(p -> "marido".equals(p.get("vinculo"))).findFirst().orElseThrow())
                .containsEntry("nombre", null).containsEntry("normalizado", null);
        assertThat(personas.stream().filter(p -> "Marta".equals(p.get("nombre"))).findFirst().orElseThrow())
                .containsEntry("normalizado", "marta");

        List<Map<String, Object>> lugares = filas(neo4j, """
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:OCURRIO_EN]->(l:Lugar)
                RETURN l.nombre AS nombre, l.nombreNormalizado AS normalizado, count(r) AS recuerdos
                """, "persona", PERSONA_A);
        // Cipolletti es un solo nodo compartido por dos recuerdos (regla 5, sin duplicados).
        assertThat(lugares).extracting(l -> l.get("nombre") + "|" + l.get("normalizado") + "|" + l.get("recuerdos"))
                .containsExactlyInAnyOrder("Cipolletti|cipolletti|2", "Escuela N° 12|escuela n° 12|1",
                        "la costa|la costa|1");
    }

    @Test
    void laHistoriaDeEjemploNoSeCargaDosVeces() {
        assertThat(carga.cargarHistoriaDeEjemplo(PERSONA_A)).isTrue();
        assertThat(carga.cargarHistoriaDeEjemplo(PERSONA_A)).isFalse();

        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                "p", PERSONA_A)).isEqualTo(4);
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->() RETURN count(r) AS c",
                "p", PERSONA_A)).isEqualTo(5);
    }

    @Test
    void laHistoriaDeEjemploNoSeCargaSiLaPersonaYaTieneProgreso() {
        // Ya respondió la pregunta 3 (por ejemplo, desde Swagger): no se le agrega nada.
        neo4j.query("""
                MERGE (p:PersonaMayor {idPersonaMayor: $p})
                WITH p MATCH (q:PreguntaGuion {idPreguntaGuion: 3})
                CREATE (p)-[:RESPONDIO {fecha: datetime()}]->(q)
                """).bind(PERSONA_A).to("p").run();

        assertThat(carga.cargarHistoriaDeEjemplo(PERSONA_A)).isFalse();

        assertThat(preguntas("RESPONDIO", PERSONA_A)).containsExactly(3);
        assertThat(preguntas("SALTEO", PERSONA_A)).isEmpty();
        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                "p", PERSONA_A)).isZero();
    }

    @Test
    void laHistoriaDeEjemploNoSeCargaSiLaPersonaYaTieneRecuerdos() {
        neo4j.query("""
                MERGE (p:PersonaMayor {idPersonaMayor: $p})
                CREATE (p)-[:TIENE_RECUERDO]->(:Recuerdo {idRecuerdo: 'test-historia-recuerdo', titulo: 'Uno propio',
                        descripcion: 'Ya tenía este.', fechaRegistro: datetime()})
                """).bind(PERSONA_A).to("p").run();

        assertThat(carga.cargarHistoriaDeEjemplo(PERSONA_A)).isFalse();

        assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                "p", PERSONA_A)).isEqualTo(1);
        assertThat(preguntas("RESPONDIO", PERSONA_A)).isEmpty();
    }

    // ---------- Aislamiento entre personas ----------

    @Test
    void dosPersonasConLaMismaPersonaYElMismoLugarCadaUnaVeSoloLoSuyo() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);
        carga.cargarHistoriaDeEjemplo(PERSONA_B);

        // Cada persona tiene su propia "Marta" y su propia "Cipolletti": nodos distintos (regla 1).
        String marta = "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:CONOCE]->(m:Persona {nombreNormalizado: 'marta'}) "
                + "RETURN m.idPersona AS id";
        List<Map<String, Object>> martaDeA = filas(neo4j, marta, "p", PERSONA_A);
        List<Map<String, Object>> martaDeB = filas(neo4j, marta, "p", PERSONA_B);
        assertThat(martaDeA).hasSize(1);
        assertThat(martaDeB).hasSize(1);
        assertThat(martaDeA.get(0).get("id")).isNotEqualTo(martaDeB.get(0).get("id"));
        for (long persona : new long[] { PERSONA_A, PERSONA_B }) {
            assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->(r) RETURN count(r) AS c",
                    "p", persona)).isEqualTo(4);
            assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:CONOCE]->(pe) RETURN count(pe) AS c",
                    "p", persona)).isEqualTo(4);
            assertThat(contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(l) "
                    + "RETURN count(DISTINCT l) AS c", "p", persona)).isEqualTo(3);
        }
        // Ningún lugar ni ninguna persona la comparten dos personas.
        assertThat(contar(neo4j, """
                MATCH (a:PersonaMayor {idPersonaMayor: $a})-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(l:Lugar)
                      <-[:OCURRIO_EN]-()<-[:TIENE_RECUERDO]-(b:PersonaMayor {idPersonaMayor: $b})
                RETURN count(l) AS c
                """, "a", PERSONA_A, "b", PERSONA_B)).isZero();
        assertThat(contar(neo4j, """
                MATCH (a:PersonaMayor {idPersonaMayor: $a})-[:CONOCE]->(pe:Persona)<-[:CONOCE]-(b:PersonaMayor {idPersonaMayor: $b})
                RETURN count(pe) AS c
                """, "a", PERSONA_A, "b", PERSONA_B)).isZero();
    }

    @Test
    void elProgresoDeUnaPersonaNoCambiaElDeOtra() {
        carga.cargarHistoriaDeEjemplo(PERSONA_A);
        neo4j.query("MERGE (:PersonaMayor {idPersonaMayor: $p})").bind(PERSONA_B).to("p").run();

        assertThat(GrafoDePrueba.proximaPregunta(neo4j, PERSONA_A)).isEqualTo(3);
        assertThat(GrafoDePrueba.proximaPregunta(neo4j, PERSONA_B)).isEqualTo(1);
    }

    private List<Integer> preguntas(String relacion, long persona) {
        return filas(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[:" + relacion
                + "]->(q:PreguntaGuion) RETURN q.idPreguntaGuion AS id", "p", persona).stream()
                .map(fila -> ((Number) fila.get("id")).intValue()).toList();
    }

    private static void assertRecuerdo(Map<String, Object> fila, String titulo, int pregunta, Long anio,
            String precision, String etapa, Long intentosAnio) {
        assertThat(fila.get("titulo")).isEqualTo(titulo);
        assertThat(((Number) fila.get("pregunta")).intValue()).isEqualTo(pregunta);
        assertThat(fila.get("anio")).isEqualTo(anio);
        assertThat(fila.get("precision")).isEqualTo(precision);
        assertThat(fila.get("etapa")).isEqualTo(etapa);
        assertThat(fila.get("intentosAnio")).isEqualTo(intentosAnio);
    }
}
