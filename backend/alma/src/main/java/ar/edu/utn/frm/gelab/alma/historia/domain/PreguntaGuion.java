package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Pregunta del guion, compartida por todas las personas (nodo de solo lectura, ver {@link PersonaMayorNodo}).
 * {@code edadDesde} y {@code edadHasta} son nulas en las preguntas sin franja de edad. {@code repreguntas}
 * es la lista ordenada de datos por los que tiene sentido repreguntar (contrato del grafo, sección 4.1).
 */
@Node("PreguntaGuion")
public class PreguntaGuion {

    @Id
    private final Integer idPreguntaGuion;
    private final Integer orden;
    private final String texto;
    private final Integer edadDesde;
    private final Integer edadHasta;
    private final List<String> repreguntas;

    public PreguntaGuion(Integer idPreguntaGuion, Integer orden, String texto, Integer edadDesde,
            Integer edadHasta, List<String> repreguntas) {
        this.idPreguntaGuion = idPreguntaGuion;
        this.orden = orden;
        this.texto = texto;
        this.edadDesde = edadDesde;
        this.edadHasta = edadHasta;
        this.repreguntas = repreguntas;
    }

    public Integer getIdPreguntaGuion() {
        return idPreguntaGuion;
    }

    public Integer getOrden() {
        return orden;
    }

    public String getTexto() {
        return texto;
    }

    public Integer getEdadDesde() {
        return edadDesde;
    }

    public Integer getEdadHasta() {
        return edadHasta;
    }

    public List<String> getRepreguntas() {
        return repreguntas;
    }
}
