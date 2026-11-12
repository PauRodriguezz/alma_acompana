package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.util.List;

/**
 * Todas las preguntas con su estado y la última respondida o salteada (contrato del grafo, 7.2).
 * {@code ultima} es nula si la persona todavía no registró ninguna.
 */
public record EstadoGuionResponse(int respondidas, int salteadas, int pendientes, List<PreguntaEstadoDto> preguntas,
        UltimaRegistradaDto ultima) {
}
