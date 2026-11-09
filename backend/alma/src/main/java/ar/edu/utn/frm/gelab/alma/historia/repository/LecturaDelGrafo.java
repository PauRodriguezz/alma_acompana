package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;

/**
 * Lecturas del guion y del progreso con Cypher explícito y mapeo propio por {@link Neo4jClient}. Un método
 * {@code @Query} de un repositorio no sirve para esto: con columnas sueltas (no un nodo), Spring Data Neo4j
 * 8.1.1 resuelve la proyección contra la entidad y falla. Toda consulta parte del nodo {@code PersonaMayor}
 * (aislamiento entre personas). Se usa dentro de la transacción de Neo4j del servicio.
 */
@Repository
public class LecturaDelGrafo {

    private final Neo4jClient neo4jClient;

    public LecturaDelGrafo(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /** Una pregunta del guion con su franja de edad y sus repreguntas, o vacío si no existe. */
    public Optional<PreguntaDelGuion> pregunta(int preguntaId) {
        return neo4jClient.query("""
                MATCH (q:PreguntaGuion {idPreguntaGuion: $pregunta})
                RETURN q.idPreguntaGuion AS id, q.orden AS orden, q.texto AS texto, q.edadDesde AS edadDesde,
                       q.edadHasta AS edadHasta, q.repreguntas AS repreguntas
                """)
                .bindAll(Map.of("pregunta", preguntaId))
                .fetchAs(PreguntaDelGuion.class)
                .mappedBy((tipos, fila) -> new PreguntaDelGuion(
                        fila.get("id").asInt(),
                        fila.get("orden").asInt(),
                        fila.get("texto").asString(),
                        fila.get("edadDesde").isNull() ? null : fila.get("edadDesde").asInt(),
                        fila.get("edadHasta").isNull() ? null : fila.get("edadHasta").asInt(),
                        fila.get("repreguntas").isNull() ? List.of()
                                : fila.get("repreguntas").asList(Value::asString)))
                .one();
    }

    /**
     * P4 del modelo: todas las preguntas, en orden, con su estado para la persona. De acá salen también la
     * próxima pendiente (P1: la primera sin relación) y la última registrada (P3: la de fecha más reciente).
     */
    public List<EstadoDePregunta> estadoDeTodas(Long personaId) {
        return neo4jClient.query("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                MATCH (q:PreguntaGuion)
                OPTIONAL MATCH (p)-[r:RESPONDIO|SALTEO]->(q)
                RETURN q.idPreguntaGuion AS id, q.orden AS orden, q.texto AS texto, type(r) AS tipo, r.fecha AS fecha
                ORDER BY q.orden
                """)
                .bindAll(Map.of("persona", personaId))
                .fetchAs(EstadoDePregunta.class)
                .mappedBy((tipos, fila) -> new EstadoDePregunta(
                        fila.get("id").asInt(),
                        fila.get("orden").asInt(),
                        fila.get("texto").asString(),
                        fila.get("tipo").isNull() ? null : fila.get("tipo").asString(),
                        fila.get("fecha").isNull() ? null : fila.get("fecha").asZonedDateTime()))
                .all().stream().toList();
    }
}
