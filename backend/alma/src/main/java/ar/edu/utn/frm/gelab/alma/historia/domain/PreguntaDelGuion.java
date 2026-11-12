package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.util.List;

/**
 * Una entrada de {@code historia/guion.json} (contrato del grafo, Anexo A). {@code edadDesde} y
 * {@code edadHasta} son nulas si la pregunta no tiene franja de edad.
 */
public record PreguntaDelGuion(int id, int orden, String texto, Integer edadDesde, Integer edadHasta,
        List<String> repreguntas) {
}
