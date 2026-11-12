package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.neo4j.core.Neo4jClient;

/**
 * Arma recuerdos directo en el grafo para las pruebas de repreguntas, con el año, la precisión y los intentos que
 * cada caso necesita (más simple y más controlable que pasar por la extracción). Todo cuelga del nodo de la persona,
 * así {@code eliminarHistoria} lo limpia.
 */
final class RecuerdosDePrueba {

    private final Neo4jClient neo4j;

    RecuerdosDePrueba(Neo4jClient neo4j) {
        this.neo4j = neo4j;
    }

    /**
     * Un recuerdo sin lugares ni personas, registrado {@code minutos} minutos después de hace una hora (así se
     * puede ordenar por antigüedad). {@code anio} y {@code precision} pueden ser nulos.
     */
    String crear(long personaId, int pregunta, String titulo, Integer anio, String precision, int intentosAnio,
            int intentosLugares, int intentosPersonas, int minutos) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("persona", personaId);
        parametros.put("pregunta", pregunta);
        parametros.put("id", id);
        parametros.put("titulo", titulo);
        parametros.put("anio", anio);
        parametros.put("precision", precision);
        parametros.put("etapa", anio == null ? null : "INFANCIA");
        parametros.put("fecha", ZonedDateTime.now().minusHours(1).plusMinutes(minutos));
        parametros.put("ia", intentosAnio);
        parametros.put("il", intentosLugares);
        parametros.put("ip", intentosPersonas);
        neo4j.query("""
                MERGE (p:PersonaMayor {idPersonaMayor: $persona})
                WITH p
                MATCH (q:PreguntaGuion {idPreguntaGuion: $pregunta})
                CREATE (r:Recuerdo {idRecuerdo: $id, titulo: $titulo, descripcion: 'Descripción original.',
                        anio: $anio, precision: $precision, etapaDeVida: $etapa, fechaRegistro: $fecha,
                        intentosAnio: $ia, intentosLugares: $il, intentosPersonas: $ip})
                CREATE (p)-[:TIENE_RECUERDO]->(r)
                CREATE (r)-[:SURGIO_DE]->(q)
                """).bindAll(parametros).run();
        return id;
    }

    /**
     * Un recuerdo sin pregunta de origen (sin la relación {@code SURGIO_DE}), como uno cargado a mano en el grafo.
     * Sin lugares ni personas, y con los tres contadores en 0.
     */
    String crearSinPreguntaDeOrigen(long personaId, String titulo, int minutos) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("persona", personaId);
        parametros.put("id", id);
        parametros.put("titulo", titulo);
        parametros.put("fecha", ZonedDateTime.now().minusHours(1).plusMinutes(minutos));
        neo4j.query("""
                MERGE (p:PersonaMayor {idPersonaMayor: $persona})
                CREATE (r:Recuerdo {idRecuerdo: $id, titulo: $titulo, descripcion: 'Descripción original.',
                        fechaRegistro: $fecha, intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0})
                CREATE (p)-[:TIENE_RECUERDO]->(r)
                """).bindAll(parametros).run();
        return id;
    }

    void agregarLugar(String recuerdoId, String nombre) {
        neo4j.query("""
                MATCH (r:Recuerdo {idRecuerdo: $recuerdo})
                CREATE (l:Lugar {idLugar: $id, nombre: $nombre, nombreNormalizado: toLower($nombre)})
                CREATE (r)-[:OCURRIO_EN]->(l)
                """).bindAll(Map.of("recuerdo", recuerdoId, "id", UUID.randomUUID().toString(), "nombre", nombre)).run();
    }

    /** Cambia la etapa de vida de un recuerdo (por defecto {@code crear} pone INFANCIA si tiene año). */
    void etapa(String recuerdoId, String etapa) {
        neo4j.query("MATCH (r:Recuerdo {idRecuerdo: $id}) SET r.etapaDeVida = $etapa")
                .bindAll(Map.of("id", recuerdoId, "etapa", etapa)).run();
    }

    /** Cambia la descripción de un recuerdo. */
    void descripcion(String recuerdoId, String descripcion) {
        neo4j.query("MATCH (r:Recuerdo {idRecuerdo: $id}) SET r.descripcion = $descripcion")
                .bindAll(Map.of("id", recuerdoId, "descripcion", descripcion)).run();
    }

    /** Agrega una persona (con {@code CONOCE {vinculo}} y {@code PARTICIPA_EN}). Nombre y vínculo pueden ser nulos. */
    void agregarPersona(long personaId, String recuerdoId, String nombre, String vinculo) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("persona", personaId);
        parametros.put("recuerdo", recuerdoId);
        parametros.put("id", UUID.randomUUID().toString());
        parametros.put("nombre", nombre);
        parametros.put("normalizado", nombre == null ? null : nombre.toLowerCase());
        parametros.put("vinculo", vinculo);
        neo4j.query("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona}), (r:Recuerdo {idRecuerdo: $recuerdo})
                CREATE (pe:Persona {idPersona: $id, nombre: $nombre, nombreNormalizado: $normalizado})
                CREATE (p)-[:CONOCE {vinculo: $vinculo}]->(pe)
                CREATE (pe)-[:PARTICIPA_EN]->(r)
                """).bindAll(parametros).run();
    }

    Map<String, Object> propiedades(String recuerdoId) {
        return filas(neo4j, """
                MATCH (r:Recuerdo {idRecuerdo: $id})
                RETURN r.titulo AS titulo, r.descripcion AS descripcion, r.anio AS anio, r.precision AS precision,
                       r.etapaDeVida AS etapa, r.intentosAnio AS intentosAnio, r.intentosLugares AS intentosLugares,
                       r.intentosPersonas AS intentosPersonas
                """, "id", recuerdoId).get(0);
    }
}
