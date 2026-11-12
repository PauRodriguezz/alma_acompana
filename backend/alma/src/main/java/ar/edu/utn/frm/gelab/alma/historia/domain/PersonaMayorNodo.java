package ar.edu.utn.frm.gelab.alma.historia.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Nodo {@code PersonaMayor} del grafo (docs/Modelo_de_grafo/modelo-de-grafo.md). Lleva solo el mismo id que
 * {@code PersonaMayor.id} en PostgreSQL: no duplica datos de perfil. El sufijo evita el choque con la
 * entidad JPA {@code persona.domain.PersonaMayor}.
 *
 * <p>Los nodos de este paquete son de solo lectura: nunca se llama a {@code save()}, porque con ids
 * asignados pisaría propiedades. Las escrituras son Cypher explícito en {@code EscrituraDelGrafo}.
 */
@Node("PersonaMayor")
public class PersonaMayorNodo {

    @Id
    private final Long idPersonaMayor;

    public PersonaMayorNodo(Long idPersonaMayor) {
        this.idPersonaMayor = idPersonaMayor;
    }

    public Long getIdPersonaMayor() {
        return idPersonaMayor;
    }
}
