package ar.edu.utn.frm.gelab.alma.historia.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

/** Repositorio solo para {@code TransaccionesNeo4jTest}; ver {@link NodoDePrueba}. */
public interface NodoDePruebaRepository extends Neo4jRepository<NodoDePrueba, String> {
}
