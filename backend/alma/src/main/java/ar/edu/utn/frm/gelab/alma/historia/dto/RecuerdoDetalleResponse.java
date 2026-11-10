package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;

/** Respuesta de {@code GET /api/historia/recuerdos/{recuerdoId}} (contrato de historia de vida, 7.4). */
public record RecuerdoDetalleResponse(String id, String titulo, String descripcion, Integer anio,
        PrecisionAnio precision, EtapaDeVida etapaDeVida, List<PersonaDeRecuerdoResponse> personas,
        List<LugarDeRecuerdoResponse> lugares) {
}
