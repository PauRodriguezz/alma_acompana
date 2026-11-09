package ar.edu.utn.frm.gelab.alma.historia.repository;

import static ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionNeo4j.MANEJADOR_NEO4J;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.NombreNormalizado;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;

/**
 * Escrituras al grafo con Cypher explícito por {@link Neo4jClient}, siempre ancladas en el nodo
 * {@code PersonaMayor} (contrato del grafo, sección 2, "Cómo se escribe"). Cada operación es una sola
 * transacción de Neo4j ({@code neo4jTransactionManager}). El log no lleva texto de la persona.
 */
@Repository
public class EscrituraDelGrafo {

    private static final Logger log = LoggerFactory.getLogger(EscrituraDelGrafo.class);

    private final Neo4jClient neo4jClient;
    private final Clock clock;

    public EscrituraDelGrafo(Neo4jClient neo4jClient, Clock clock) {
        this.neo4jClient = neo4jClient;
        this.clock = clock;
    }

    /**
     * Crea las restricciones de unicidad. Va sin transacción: Neo4j no deja mezclar esquema y datos.
     * Cada sentencia lleva {@code IF NOT EXISTS}, así que repetirla no hace nada.
     */
    public void crearRestricciones(List<String> sentencias) {
        for (String sentencia : sentencias) {
            neo4jClient.query(sentencia).run();
        }
    }

    /**
     * Crea solo las preguntas que no existen (por {@code idPreguntaGuion}) y no modifica las que ya están: la
     * fuente de verdad pasa a ser el grafo (contrato, sección 2, "Guion").
     *
     * @return cuántas preguntas se crearon
     */
    @Transactional(MANEJADOR_NEO4J)
    public int crearPreguntasFaltantes(List<PreguntaDelGuion> preguntas) {
        List<Map<String, Object>> filas = preguntas.stream().map(p -> {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id", p.id());
            fila.put("orden", p.orden());
            fila.put("texto", p.texto());
            fila.put("edadDesde", p.edadDesde());
            fila.put("edadHasta", p.edadHasta());
            fila.put("repreguntas", p.repreguntas());
            return fila;
        }).toList();
        return neo4jClient.query("""
                UNWIND $preguntas AS p
                MERGE (q:PreguntaGuion {idPreguntaGuion: p.id})
                ON CREATE SET q.orden = p.orden, q.texto = p.texto, q.edadDesde = p.edadDesde,
                              q.edadHasta = p.edadHasta, q.repreguntas = p.repreguntas
                """)
                .bindAll(parametros("preguntas", filas))
                .run().counters().nodesCreated();
    }

    /** ¿La persona ya tiene algún recuerdo o alguna pregunta respondida o salteada? */
    @Transactional(MANEJADOR_NEO4J)
    public boolean tieneRecuerdosOProgreso(Long personaId) {
        return neo4jClient.query("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                RETURN EXISTS { (p)-[:TIENE_RECUERDO|RESPONDIO|SALTEO]->() } AS tiene
                """)
                .bindAll(parametros("persona", personaId))
                .fetch().one()
                .map(fila -> Boolean.TRUE.equals(fila.get("tiene")))
                .orElse(false);
    }

    /**
     * Carga la historia de ejemplo del Anexo B del contrato: las preguntas 1, 2, 7 y 17 respondidas, la 8
     * salteada (la 3 queda como próxima pendiente) y cuatro recuerdos. Solo la carga si la persona no tiene
     * recuerdos ni progreso, para no chocar con lo que ya contestó (regla 3). Requiere el guion cargado.
     *
     * @return {@code true} si la cargó; {@code false} si la persona ya tenía historia
     */
    @Transactional(MANEJADOR_NEO4J)
    public boolean cargarHistoriaDeEjemplo(Long personaId) {
        if (tieneRecuerdosOProgreso(personaId)) {
            return false;
        }
        ZonedDateTime ahora = ZonedDateTime.now(clock);
        ejecutar("MERGE (:PersonaMayor {idPersonaMayor: $persona})", parametros("persona", personaId));

        registrarProgreso(personaId, "RESPONDIO", List.of(
                progreso(1, ahora.minusMinutes(50)), progreso(2, ahora.minusMinutes(40)),
                progreso(7, ahora.minusMinutes(30)), progreso(17, ahora.minusMinutes(10))));
        registrarProgreso(personaId, "SALTEO", List.of(progreso(8, ahora.minusMinutes(20))));

        String cipolletti = crearLugar("Cipolletti");

        String nacimiento = crearRecuerdo(personaId, 1, ahora.minusMinutes(49), "Nací en Cipolletti",
                "Nació en Cipolletti.", 1950, PrecisionAnio.ESTIMADA, EtapaDeVida.INFANCIA, 0);
        enlazarLugar(nacimiento, cipolletti);

        String casa = crearRecuerdo(personaId, 2, ahora.minusMinutes(39), "La casa de la calle San Martín",
                "Una casa chica con patio, donde vivía con su madre y su hermana.", 1958,
                PrecisionAnio.APROXIMADA, EtapaDeVida.INFANCIA, 0);
        crearPersona(personaId, casa, "Marta", "hermana");
        crearPersona(personaId, casa, "Rosa", "madre");
        enlazarLugar(casa, cipolletti);

        String maestra = crearRecuerdo(personaId, 7, ahora.minusMinutes(29), "Mi maestra Elena",
                "Su maestra de primer grado, que le enseñó a leer.", 1957, PrecisionAnio.EXACTA,
                EtapaDeVida.INFANCIA, 0);
        crearPersona(personaId, maestra, "Elena", "maestra");
        enlazarLugar(maestra, crearLugar("Escuela N° 12"));

        String viaje = crearRecuerdo(personaId, 17, ahora.minusMinutes(9), "Un viaje a la costa",
                "Un viaje a la costa con su marido, con el recuerdo del mar.", null, null, null, 1);
        crearPersona(personaId, viaje, null, "marido");
        enlazarLugar(viaje, crearLugar("la costa"));

        log.info("Historia de ejemplo cargada para la persona {}", personaId);
        return true;
    }

    /**
     * Baja de la persona (regla de integridad 8): borra su nodo y todos sus recuerdos, personas y lugares.
     * No toca las preguntas del guion ni los datos de otras personas. Se puede repetir sin error: si la
     * persona no tiene nodo, no hace nada.
     */
    @Transactional(MANEJADOR_NEO4J)
    public void eliminarSubgrafo(Long personaId) {
        Map<String, Object> parametros = parametros("persona", personaId);
        ejecutar("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(l:Lugar)
                DETACH DELETE l
                """, parametros);
        ejecutar("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:CONOCE]->(pe:Persona)
                DETACH DELETE pe
                """, parametros);
        ejecutar("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)
                DETACH DELETE r
                """, parametros);
        ejecutar("MATCH (p:PersonaMayor {idPersonaMayor: $persona}) DETACH DELETE p", parametros);
    }

    // ---------- Historia de ejemplo: piezas ----------

    private static Map<String, Object> progreso(int pregunta, ZonedDateTime fecha) {
        return parametros("pregunta", pregunta, "fecha", fecha);
    }

    private void registrarProgreso(Long personaId, String relacion, List<Map<String, Object>> filas) {
        int creadas = ejecutar("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                UNWIND $filas AS f
                MATCH (q:PreguntaGuion {idPreguntaGuion: f.pregunta})
                CREATE (p)-[:%s {fecha: f.fecha}]->(q)
                """.formatted(relacion), parametros("persona", personaId, "filas", filas))
                .counters().relationshipsCreated();
        if (creadas != filas.size()) {
            throw new IllegalStateException("Falta el guion: se esperaban " + filas.size() + " preguntas y hubo "
                    + creadas);
        }
    }

    private String crearRecuerdo(Long personaId, int pregunta, ZonedDateTime registro, String titulo,
            String descripcion, Integer anio, PrecisionAnio precision, EtapaDeVida etapa, int intentosAnio) {
        String id = UUID.randomUUID().toString();
        int relaciones = ejecutar("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                MATCH (q:PreguntaGuion {idPreguntaGuion: $pregunta})
                CREATE (r:Recuerdo {idRecuerdo: $id, titulo: $titulo, descripcion: $descripcion, anio: $anio,
                                    precision: $precision, etapaDeVida: $etapa, fechaRegistro: $registro,
                                    intentosAnio: $intentosAnio, intentosLugares: 0, intentosPersonas: 0})
                CREATE (p)-[:TIENE_RECUERDO]->(r)
                CREATE (r)-[:SURGIO_DE]->(q)
                """, parametros("persona", personaId, "pregunta", pregunta, "id", id, "titulo", titulo,
                "descripcion", descripcion, "anio", anio, "precision", precision != null ? precision.name() : null,
                "etapa", etapa != null ? etapa.name() : null, "registro", registro, "intentosAnio", intentosAnio))
                .counters().relationshipsCreated();
        if (relaciones != 2) {
            throw new IllegalStateException("Falta el guion o la persona: no se pudo crear el recuerdo");
        }
        return id;
    }

    private String crearLugar(String nombre) {
        String id = UUID.randomUUID().toString();
        ejecutar("CREATE (:Lugar {idLugar: $id, nombre: $nombre, nombreNormalizado: $normalizado})",
                parametros("id", id, "nombre", nombre, "normalizado", NombreNormalizado.de(nombre)));
        return id;
    }

    private void enlazarLugar(String recuerdoId, String lugarId) {
        ejecutar("""
                MATCH (r:Recuerdo {idRecuerdo: $recuerdo}), (l:Lugar {idLugar: $lugar})
                CREATE (r)-[:OCURRIO_EN]->(l)
                """, parametros("recuerdo", recuerdoId, "lugar", lugarId));
    }

    private void crearPersona(Long personaId, String recuerdoId, String nombre, String vinculo) {
        ejecutar("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona}), (r:Recuerdo {idRecuerdo: $recuerdo})
                CREATE (pe:Persona {idPersona: $id, nombre: $nombre, nombreNormalizado: $normalizado})
                CREATE (p)-[:CONOCE {vinculo: $vinculo}]->(pe)
                CREATE (pe)-[:PARTICIPA_EN]->(r)
                """, parametros("persona", personaId, "recuerdo", recuerdoId, "id", UUID.randomUUID().toString(),
                "nombre", nombre, "normalizado", NombreNormalizado.de(nombre), "vinculo", vinculo));
    }

    // ---------- Utilidades ----------

    private org.neo4j.driver.summary.ResultSummary ejecutar(String cypher, Map<String, Object> parametros) {
        return neo4jClient.query(cypher).bindAll(parametros).run();
    }

    /** Pares nombre, valor. Admite valores nulos, que {@code Map.of} no admite. */
    private static Map<String, Object> parametros(Object... paresNombreValor) {
        Map<String, Object> parametros = new HashMap<>();
        for (int i = 0; i < paresNombreValor.length; i += 2) {
            parametros.put((String) paresNombreValor[i], paresNombreValor[i + 1]);
        }
        return parametros;
    }
}
