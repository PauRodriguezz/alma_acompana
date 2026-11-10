package ar.edu.utn.frm.gelab.alma.historia.dto;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;

/**
 * Un recuerdo en la línea de tiempo (contrato de historia de vida, 7.3). {@code anio}, {@code precision} y
 * {@code etapaDeVida} son nulos si no tiene año; {@code resumen} es la descripción cortada a 140 caracteres.
 */
public record RecuerdoResumenResponse(String id, String titulo, Integer anio, PrecisionAnio precision,
        EtapaDeVida etapaDeVida, String resumen) {
}
