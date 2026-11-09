package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaGuion;

/**
 * Lecturas del guion. Solo lectura: nunca se llama a {@code save()} (ver {@link PreguntaGuion}); las
 * escrituras son Cypher explícito en {@link EscrituraDelGrafo}.
 */
public interface PreguntaGuionRepository extends Neo4jRepository<PreguntaGuion, Integer> {

    List<PreguntaGuion> findAllByOrderByOrdenAsc();
}
