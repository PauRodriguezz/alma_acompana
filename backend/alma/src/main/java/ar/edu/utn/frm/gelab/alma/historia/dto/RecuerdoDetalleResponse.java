package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de {@code GET /api/historia/recuerdos/{recuerdoId}} (contrato de historia de vida, 7.4, y del mapa de
 * vida, 3.2).
 */
public record RecuerdoDetalleResponse(String id, String titulo, String descripcion, Integer anio,
        PrecisionAnio precision, EtapaDeVida etapaDeVida, List<PersonaDeRecuerdoResponse> personas,
        List<LugarDeRecuerdoResponse> lugares,
        @Schema(description = "Datos que se pueden seguir completando en este recuerdo; igual que en el listado.")
        List<DatoFaltante> faltantes,
        @Schema(description = "Texto de la pregunta del guion de la que surgió el recuerdo; nulo si no se encuentra.",
                nullable = true) String preguntaTexto) {
}
