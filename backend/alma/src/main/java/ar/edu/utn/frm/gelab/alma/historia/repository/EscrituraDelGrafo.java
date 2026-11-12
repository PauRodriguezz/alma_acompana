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

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatosParaCompletar;
import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.NombreNormalizado;
import ar.edu.utn.frm.gelab.alma.historia.domain.NuevoRecuerdo;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import ar.edu.utn.frm.gelab.alma.historia.domain.TipoProgreso;

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

    /** Resultado de {@link #registrarProgreso}. */
    public enum ResultadoRegistro {
        REGISTRADO,
        PREGUNTA_INEXISTENTE,
        /** La persona ya había respondido o salteado esa pregunta (regla de integridad 3). */
        YA_REGISTRADA
    }

    /** {@code MERGE} del nodo de la persona: se crea la primera vez que usa el módulo y no guarda otros datos. */
    @Transactional(MANEJADOR_NEO4J)
    public void asegurarPersona(Long personaId) {
        ejecutar("MERGE (:PersonaMayor {idPersonaMayor: $persona})", parametros("persona", personaId));
    }

    /**
     * Registra {@code RESPONDIO} o {@code SALTEO} con la fecha de ahora, en una sola transacción:
     * <ol>
     *   <li>{@code MERGE} del nodo de la persona;</li>
     *   <li><b>bloqueo</b> de ese nodo ({@link #bloquearPersona}), que se mantiene hasta el final de la
     *       transacción: dos pedidos de la misma persona se atienden uno detrás del otro y no pueden pasar a
     *       la vez el chequeo del paso 4 (contrato, sección 2, "Concurrencia en responder y saltear");</li>
     *   <li>la pregunta existe;</li>
     *   <li>una sola consulta que crea la relación solo si la persona no tiene {@code RESPONDIO} ni
     *       {@code SALTEO} hacia esa pregunta.</li>
     * </ol>
     */
    @Transactional(MANEJADOR_NEO4J)
    public ResultadoRegistro registrarProgreso(Long personaId, int preguntaId, TipoProgreso tipo) {
        asegurarPersona(personaId);
        bloquearPersona(personaId);
        long preguntas = neo4jClient.query("MATCH (q:PreguntaGuion {idPreguntaGuion: $pregunta}) RETURN count(q) AS c")
                .bindAll(parametros("pregunta", preguntaId)).fetch().one()
                .map(fila -> ((Number) fila.get("c")).longValue()).orElse(0L);
        if (preguntas == 0) {
            return ResultadoRegistro.PREGUNTA_INEXISTENTE;
        }
        int creadas = ejecutar("""
                MATCH (p:PersonaMayor {idPersonaMayor: $persona})
                MATCH (q:PreguntaGuion {idPreguntaGuion: $pregunta})
                WHERE NOT EXISTS { (p)-[:RESPONDIO|SALTEO]->(q) }
                CREATE (p)-[:%s {fecha: $fecha}]->(q)
                """.formatted(tipo.name()),
                parametros("persona", personaId, "pregunta", preguntaId, "fecha", ZonedDateTime.now(clock)))
                .counters().relationshipsCreated();
        return creadas == 1 ? ResultadoRegistro.REGISTRADO : ResultadoRegistro.YA_REGISTRADA;
    }

    /**
     * Toma el bloqueo de escritura del nodo de la persona con una escritura que no cambia nada
     * ({@code SET p.idPersonaMayor = p.idPersonaMayor}), así el nodo no suma propiedades. El bloqueo dura
     * hasta que termina la transacción. Es {@code protected} para que la prueba de concurrencia pueda
     * correr el mismo trabajo sin bloqueo (control).
     */
    protected void bloquearPersona(Long personaId) {
        ejecutar("MATCH (p:PersonaMayor {idPersonaMayor: $persona}) SET p.idPersonaMayor = p.idPersonaMayor",
                parametros("persona", personaId));
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

    /**
     * Escribe un recuerdo nuevo con sus personas y lugares (contrato, 5.6), todo en <b>una sola transacción</b>
     * y anclado en el nodo de la persona. Primero bloquea ese nodo, como responder y saltear: dos escrituras de
     * la misma persona se atienden una detrás de la otra, así que "buscar y si no existe crear" no puede
     * duplicar personas ni lugares (regla de integridad 5). Si la persona ya no tiene nodo (por ejemplo, se
     * dio de baja mientras tanto) o falta la pregunta, lanza y no queda nada escrito.
     *
     * @return el id del recuerdo
     */
    @Transactional(MANEJADOR_NEO4J)
    public String registrarRecuerdo(Long personaId, NuevoRecuerdo nuevo) {
        bloquearPersona(personaId);
        String recuerdoId = crearRecuerdo(personaId, nuevo.preguntaId(), ZonedDateTime.now(clock), nuevo.titulo(),
                nuevo.descripcion(), nuevo.anio(), nuevo.precision(), nuevo.etapa(), 0);
        for (NuevoRecuerdo.PersonaNueva persona : nuevo.personas()) {
            enlazarOCrearPersona(personaId, recuerdoId, persona);
        }
        for (String lugar : nuevo.lugares()) {
            String existente = buscarLugar(personaId, NombreNormalizado.de(lugar));
            enlazarLugar(recuerdoId, existente != null ? existente : crearLugar(lugar));
        }
        return recuerdoId;
    }

    /** Resultado de {@link #registrarIntento}. */
    public enum ResultadoIntento {
        REGISTRADO,
        /** El recuerdo no existe o no es de la persona. */
        RECUERDO_INEXISTENTE,
        /** El dato no aplica a ese recuerdo, ya no falta o ya se preguntó el máximo de veces. */
        NO_CORRESPONDE
    }

    /**
     * Cuenta un intento (responder o saltear una repregunta, contrato 5.7) en una sola transacción: bloquea el
     * nodo de la persona (como responder y saltear el guion: dos pedidos a la vez no pueden pasar los dos el
     * chequeo del máximo) y suma 1 al contador del dato con una sola consulta que solo lo hace si el dato está en
     * la lista {@code repreguntas} de la pregunta de origen, si todavía falta y si el contador vale menos de
     * {@code maximo}. No crea preguntas ni toca el progreso.
     */
    @Transactional(MANEJADOR_NEO4J)
    public ResultadoIntento registrarIntento(Long personaId, String recuerdoId, DatoFaltante dato, int maximo) {
        bloquearPersona(personaId);
        if (!existeRecuerdo(personaId, recuerdoId)) {
            return ResultadoIntento.RECUERDO_INEXISTENTE;
        }
        String contador = CypherRepreguntas.propiedadIntentos(dato);
        int propiedades = ejecutar("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo {idRecuerdo: $recuerdo})
                      -[:SURGIO_DE]->(q:PreguntaGuion)
                WHERE $dato IN q.repreguntas AND coalesce(r.%1$s, 0) < $maximo AND %2$s
                SET r.%1$s = coalesce(r.%1$s, 0) + 1
                """.formatted(contador, CypherRepreguntas.condicionFalta(dato)),
                parametros("persona", personaId, "recuerdo", recuerdoId, "dato", dato.name(), "maximo", maximo))
                .counters().propertiesSet();
        return propiedades == 1 ? ResultadoIntento.REGISTRADO : ResultadoIntento.NO_CORRESPONDE;
    }

    /**
     * Devuelve un intento (contrato, 5.7): cuando la extracción de una repregunta falla porque el modelo no estaba
     * disponible, el dato no se gastó. Resta 1 al contador de ese dato y nunca lo deja por debajo de 0. Una
     * transacción, con el nodo de la persona bloqueado (igual que {@link #registrarIntento}). Un recuerdo que ya no
     * existe o no es de la persona no cambia nada.
     */
    @Transactional(MANEJADOR_NEO4J)
    public void devolverIntento(Long personaId, String recuerdoId, DatoFaltante dato) {
        bloquearPersona(personaId);
        String contador = CypherRepreguntas.propiedadIntentos(dato);
        ejecutar("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo {idRecuerdo: $recuerdo})
                WHERE coalesce(r.%1$s, 0) > 0
                SET r.%1$s = r.%1$s - 1
                """.formatted(contador), parametros("persona", personaId, "recuerdo", recuerdoId));
    }

    /**
     * Aplica la respuesta de una repregunta a un recuerdo ya creado (contrato, 5.7), en una transacción y con el
     * nodo de la persona bloqueado. Solo toca lo del dato preguntado y nunca el título ni la descripción:
     * <ul>
     *   <li>{@code ANIO}: fija año, precisión y etapa solo si el recuerdo no tiene año o es {@code ESTIMADA};
     *       nunca pisa un año {@code EXACTA}, {@code APROXIMADA} o {@code CALCULADA_POR_EDAD};</li>
     *   <li>{@code LUGARES} y {@code PERSONAS}: suma los nuevos, reutilizando los que ya existen.</li>
     * </ul>
     * Si el recuerdo ya no existe (por ejemplo, la persona se dio de baja), no hace nada.
     */
    @Transactional(MANEJADOR_NEO4J)
    public void completarRecuerdo(Long personaId, String recuerdoId, DatosParaCompletar datos) {
        bloquearPersona(personaId);
        if (!existeRecuerdo(personaId, recuerdoId)) {
            return;
        }
        switch (datos.dato()) {
            case ANIO -> {
                if (datos.anio() != null) {
                    ejecutar("""
                            MATCH (r:Recuerdo {idRecuerdo: $recuerdo})
                            WHERE r.anio IS NULL OR r.precision = 'ESTIMADA'
                            SET r.anio = $anio, r.precision = $precision, r.etapaDeVida = $etapa
                            """, parametros("recuerdo", recuerdoId, "anio", datos.anio(), "precision",
                            datos.precision().name(), "etapa", datos.etapa().name()));
                }
            }
            case LUGARES -> {
                for (String lugar : datos.lugares()) {
                    String existente = buscarLugar(personaId, NombreNormalizado.de(lugar));
                    enlazarLugar(recuerdoId, existente != null ? existente : crearLugar(lugar));
                }
            }
            case PERSONAS -> {
                for (NuevoRecuerdo.PersonaNueva persona : datos.personas()) {
                    enlazarOCrearPersona(personaId, recuerdoId, persona);
                }
            }
        }
    }

    private boolean existeRecuerdo(Long personaId, String recuerdoId) {
        return neo4jClient.query("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo {idRecuerdo: $recuerdo})
                RETURN count(r) AS c
                """).bindAll(parametros("persona", personaId, "recuerdo", recuerdoId)).fetch().one()
                .map(fila -> ((Number) fila.get("c")).longValue() > 0).orElse(false);
    }

    /**
     * Con nombre: se reutiliza la persona con ese {@code nombreNormalizado} (P6) sin cambiarle el vínculo.
     * Sin nombre: se reutiliza si hay exactamente una persona con ese vínculo (P7). Si no, se crea.
     */
    private void enlazarOCrearPersona(Long personaId, String recuerdoId, NuevoRecuerdo.PersonaNueva persona) {
        String normalizado = NombreNormalizado.de(persona.nombre());
        String existente = null;
        if (normalizado != null) {
            existente = unico(neo4jClient.query("""
                    MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:CONOCE]->(pe:Persona {nombreNormalizado: $nombre})
                    RETURN pe.idPersona AS id
                    """).bindAll(parametros("persona", personaId, "nombre", normalizado)));
        } else if (persona.vinculo() != null) {
            List<String> conVinculo = ids(neo4jClient.query("""
                    MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:CONOCE {vinculo: $vinculo}]->(pe:Persona)
                    RETURN pe.idPersona AS id
                    """).bindAll(parametros("persona", personaId, "vinculo", persona.vinculo())));
            existente = conVinculo.size() == 1 ? conVinculo.get(0) : null;
        }
        if (existente == null) {
            crearPersona(personaId, recuerdoId, persona.nombre(), persona.vinculo());
        } else {
            ejecutar("""
                    MATCH (pe:Persona {idPersona: $persona}), (r:Recuerdo {idRecuerdo: $recuerdo})
                    MERGE (pe)-[:PARTICIPA_EN]->(r)
                    """, parametros("persona", existente, "recuerdo", recuerdoId));
        }
    }

    /** P8: el lugar con ese {@code nombreNormalizado} entre los recuerdos de la persona, o {@code null}. */
    private String buscarLugar(Long personaId, String normalizado) {
        return unico(neo4jClient.query("""
                MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(:Recuerdo)
                      -[:OCURRIO_EN]->(l:Lugar {nombreNormalizado: $nombre})
                RETURN DISTINCT l.idLugar AS id
                """).bindAll(parametros("persona", personaId, "nombre", normalizado)));
    }

    private static List<String> ids(Neo4jClient.RunnableSpec consulta) {
        return consulta.fetch().all().stream().map(fila -> (String) fila.get("id")).distinct().toList();
    }

    private static String unico(Neo4jClient.RunnableSpec consulta) {
        List<String> ids = ids(consulta);
        return ids.isEmpty() ? null : ids.get(0);
    }

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
                MERGE (r)-[:OCURRIO_EN]->(l)
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
