package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.time.OffsetDateTime;

import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoPregunta;

/** La última pregunta respondida o salteada (contrato del grafo, 7.2). {@code estado} no es nunca PENDIENTE. */
public record UltimaRegistradaDto(Integer preguntaId, EstadoPregunta estado, OffsetDateTime fecha) {
}
