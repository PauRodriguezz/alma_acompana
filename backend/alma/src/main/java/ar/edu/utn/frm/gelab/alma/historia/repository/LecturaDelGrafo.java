package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.neo4j.driver.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
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
                RETURN %s
                """.formatted(COLUMNAS_DE_PREGUNTA))
                .bindAll(Map.of("pregunta", preguntaId))
                .fetchAs(PreguntaDelGuion.class)
                .mappedBy((tipos, fila) -> aPregunta(fila))
                .one();
    }

    /**
     * La pregunta del guion de la que surgió un recuerdo de la persona, o vacío si el recuerdo no existe o no es
     * suyo (la consulta parte de su nodo).
     */
    public Optional<PreguntaDelGuion> preguntaDeOrigen(Long personaId, String recuerdoId) {
        return neo4jClient.query("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(:Recuerdo {idRecuerdo: $recuerdo})
                      -[:SURGIO_DE]->(q:PreguntaGuion)
                RETURN %s
                """.formatted(COLUMNAS_DE_PREGUNTA))
                .bindAll(Map.of("persona", personaId, "recuerdo", recuerdoId))
                .fetchAs(PreguntaDelGuion.class)
                .mappedBy((tipos, fila) -> aPregunta(fila))
                .one();
    }

    /**
     * Los recuerdos de la persona con lo necesario para elegir una repregunta (contrato, 5.7 y 7.5): la lista
     * {@code repreguntas} de su pregunta de origen, qué datos faltan hoy y cuántas veces se preguntó por cada
     * uno. Del más antiguo al más nuevo ({@code fechaRegistro}). Con {@code preguntaId}, solo los que surgieron de
     * esa pregunta. Toda la consulta parte del nodo de la persona.
     */
    public List<RecuerdoParaRepreguntar> recuerdosParaRepreguntar(Long personaId, Integer preguntaId) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("persona", personaId);
        parametros.put("pregunta", preguntaId);
        return neo4jClient.query("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
                WHERE $pregunta IS NULL OR q.idPreguntaGuion = $pregunta
                RETURN r.idRecuerdo AS id, r.fechaRegistro AS fecha, q.repreguntas AS repreguntas,
                       %s AS faltaAnio, %s AS faltaLugares, %s AS faltaPersonas,
                       coalesce(r.intentosAnio, 0) AS intentosAnio,
                       coalesce(r.intentosLugares, 0) AS intentosLugares,
                       coalesce(r.intentosPersonas, 0) AS intentosPersonas
                ORDER BY r.fechaRegistro, r.idRecuerdo
                """.formatted(CypherRepreguntas.condicionFalta(DatoFaltante.ANIO),
                CypherRepreguntas.condicionFalta(DatoFaltante.LUGARES),
                CypherRepreguntas.condicionFalta(DatoFaltante.PERSONAS)))
                .bindAll(parametros)
                .fetchAs(RecuerdoParaRepreguntar.class)
                .mappedBy((tipos, fila) -> new RecuerdoParaRepreguntar(
                        fila.get("id").asString(),
                        fila.get("fecha").isNull() ? null : fila.get("fecha").asZonedDateTime(),
                        fila.get("repreguntas").isNull() ? List.<DatoFaltante>of()
                                : fila.get("repreguntas").asList(Value::asString).stream()
                                        .map(LecturaDelGrafo::dato).filter(Objects::nonNull).toList(),
                        fila.get("faltaAnio").asBoolean(), fila.get("faltaLugares").asBoolean(),
                        fila.get("faltaPersonas").asBoolean(),
                        fila.get("intentosAnio").asInt(), fila.get("intentosLugares").asInt(),
                        fila.get("intentosPersonas").asInt()))
                .all().stream().toList();
    }

    private static final String COLUMNAS_DE_PREGUNTA = """
            q.idPreguntaGuion AS id, q.orden AS orden, q.texto AS texto, q.edadDesde AS edadDesde,
            q.edadHasta AS edadHasta, q.repreguntas AS repreguntas""";

    private static PreguntaDelGuion aPregunta(org.neo4j.driver.Record fila) {
        return new PreguntaDelGuion(
                fila.get("id").asInt(),
                fila.get("orden").asInt(),
                fila.get("texto").asString(),
                fila.get("edadDesde").isNull() ? null : fila.get("edadDesde").asInt(),
                fila.get("edadHasta").isNull() ? null : fila.get("edadHasta").asInt(),
                fila.get("repreguntas").isNull() ? List.of() : fila.get("repreguntas").asList(Value::asString));
    }

    /** Un texto de la lista {@code repreguntas} como dato; un valor desconocido se ignora. */
    private static DatoFaltante dato(String texto) {
        try {
            return DatoFaltante.valueOf(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
