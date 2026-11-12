package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoRepregunta;

/**
 * Respuesta de {@code GET /api/historia/repreguntas/siguiente} (contrato del grafo, 7.5). {@code repregunta} solo
 * viene con el estado {@code HAY}; con {@code PROCESANDO} y {@code NINGUNA} es nula.
 */
public record RepreguntaSiguienteResponse(EstadoRepregunta estado, RepreguntaDto repregunta) {
}
