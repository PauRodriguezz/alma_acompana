package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoPregunta;

/** Una pregunta del guion con su estado para la persona (contrato del grafo, 7.2). */
public record PreguntaEstadoDto(Integer id, Integer orden, String texto, EstadoPregunta estado) {
}
