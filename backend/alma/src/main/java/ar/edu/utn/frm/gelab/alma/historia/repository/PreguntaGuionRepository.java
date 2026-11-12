package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaGuion;

/**
 * Lecturas del nodo {@link PreguntaGuion} como entidad. Solo lectura: nunca se llama a {@code save()} (ver
 * {@link PreguntaGuion}); las escrituras son Cypher explícito en {@link EscrituraDelGrafo}. Las lecturas que
 * devuelven columnas sueltas, como el estado de las preguntas, están en {@link LecturaDelGrafo}.
 */
public interface PreguntaGuionRepository extends Neo4jRepository<PreguntaGuion, Integer> {

    List<PreguntaGuion> findAllByOrderByOrdenAsc();
}
