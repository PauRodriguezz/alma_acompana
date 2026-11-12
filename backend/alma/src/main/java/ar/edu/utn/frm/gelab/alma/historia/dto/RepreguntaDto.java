package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una repregunta candidata: el recuerdo, el dato que falta, el texto fijo que se le muestra (contrato del grafo, 7.5)
 * y el título del recuerdo (contrato del mapa de vida, 3.3).
 */
public record RepreguntaDto(String recuerdoId, DatoFaltante dato, String texto,
        @Schema(description = "Título del recuerdo al que corresponde la repregunta; puede ser nulo.", nullable = true)
        String tituloRecuerdo) {
}
