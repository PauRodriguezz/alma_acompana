package ar.edu.utn.frm.gelab.alma.historia.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Un recuerdo en la línea de tiempo (contrato de historia de vida, 7.3, y del mapa de vida, 3.1). {@code anio},
 * {@code precision} y {@code etapaDeVida} son nulos si no tiene año; {@code resumen} es la descripción cortada a 140
 * caracteres.
 */
public record RecuerdoResumenResponse(String id, String titulo, Integer anio, PrecisionAnio precision,
        EtapaDeVida etapaDeVida, String resumen,
        @Schema(description = "Datos que se pueden seguir completando en este recuerdo (ANIO, LUGARES, PERSONAS), en el "
                + "orden de las repreguntas de su pregunta de origen. Un dato figura si está en esa lista, falta y se "
                + "preguntó menos de 2 veces. Vacía si no hay ninguno.") List<DatoFaltante> faltantes) {
}
