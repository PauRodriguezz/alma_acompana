package ar.edu.utn.frm.gelab.alma.historia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.Neo4jClient;

/**
 * Ayudas para las pruebas del grafo: consultas de lectura sobre el Neo4j de {@code docker compose up -d}.
 * Los ids de persona de prueba son negativos, así que no pueden chocar con los de PostgreSQL.
 */
final class GrafoDePrueba {

    /** Ids de las personas de prueba del grafo (sin fila en PostgreSQL). */
    static final long PERSONA_A = -1L;
    static final long PERSONA_B = -2L;

    /** Las preguntas de prueba llevan ids desde 9001; el guion real va del 1 al 21. */
    static final int PRIMERA_PREGUNTA_DE_PRUEBA = 9000;

    private GrafoDePrueba() {
    }

    static List<Map<String, Object>> filas(Neo4jClient neo4j, String cypher, Object... paresNombreValor) {
        return new ArrayList<>(neo4j.query(cypher).bindAll(parametros(paresNombreValor)).fetch().all());
    }

    static long contar(Neo4jClient neo4j, String cypher, Object... paresNombreValor) {
        return ((Number) neo4j.query(cypher).bindAll(parametros(paresNombreValor)).fetch().one().orElseThrow()
                .get("c")).longValue();
    }

    /** Borra las preguntas de prueba (id desde 9001) con sus relaciones. Se llama antes y después de cada prueba. */
    static void borrarPreguntasDePrueba(Neo4jClient neo4j) {
        neo4j.query("MATCH (q:PreguntaGuion) WHERE q.idPreguntaGuion > $desde DETACH DELETE q")
                .bind(PRIMERA_PREGUNTA_DE_PRUEBA).to("desde").run();
    }

    /** La próxima pregunta pendiente (consulta P1 del modelo) o {@code null}. */
    static Integer proximaPregunta(Neo4jClient neo4j, long personaId) {
        List<Map<String, Object>> filas = filas(neo4j, """
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                MATCH (q:PreguntaGuion)
                WHERE NOT EXISTS { (p)-[:RESPONDIO|SALTEO]->(q) }
                RETURN q.idPreguntaGuion AS id ORDER BY q.orden LIMIT 1
                """, "persona", personaId);
        return filas.isEmpty() ? null : ((Number) filas.get(0).get("id")).intValue();
    }

    private static Map<String, Object> parametros(Object... paresNombreValor) {
        Map<String, Object> parametros = new HashMap<>();
        for (int i = 0; i < paresNombreValor.length; i += 2) {
            parametros.put((String) paresNombreValor[i], paresNombreValor[i + 1]);
        }
        return parametros;
    }
}
