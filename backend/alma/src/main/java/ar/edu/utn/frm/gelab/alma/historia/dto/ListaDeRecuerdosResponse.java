package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.util.List;

/** Respuesta de {@code GET /api/historia/recuerdos} (contrato de historia de vida, 7.3). */
public record ListaDeRecuerdosResponse(List<RecuerdoResumenResponse> recuerdos) {
}
