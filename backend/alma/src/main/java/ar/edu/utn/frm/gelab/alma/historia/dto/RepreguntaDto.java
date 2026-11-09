package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;

/** Una repregunta candidata: el recuerdo, el dato que falta y el texto fijo que se le muestra (contrato, 7.5). */
public record RepreguntaDto(String recuerdoId, DatoFaltante dato, String texto) {
}
