package ar.edu.utn.frm.gelab.alma.historia.repository;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Nodo solo para {@code TransaccionesNeo4jTest}: comprueba que un repositorio de Neo4j (el paquete que
 * fija {@code ConfiguracionNeo4j}) usa el manejador de Neo4j. Lleva la etiqueta {@code PruebaTx}, que no
 * es del modelo de grafo, y la prueba lo borra al terminar. Se puede borrar junto con su repositorio
 * cuando otra prueba cubra eso con los repositorios reales de {@code historia/}.
 */
@Node("PruebaTx")
public class NodoDePrueba {

    @Id
    private String id;

    private String marca;

    public NodoDePrueba() {
    }

    public NodoDePrueba(String id, String marca) {
        this.id = id;
        this.marca = marca;
    }

    public String getId() {
        return id;
    }

    public String getMarca() {
        return marca;
    }
}
