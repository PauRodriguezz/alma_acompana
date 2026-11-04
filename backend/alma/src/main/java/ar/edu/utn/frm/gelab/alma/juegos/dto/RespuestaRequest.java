package ar.edu.utn.frm.gelab.alma.juegos.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/juegos/respuesta (contrato de juegos, 6.2 y 5.6). En opción múltiple va opcionId;
 * en ordenar, ordenOpcionIds con todos los pasos en el orden elegido. Lo que depende del formato lo valida
 * el servicio.
 */
public record RespuestaRequest(
        @NotNull(message = MensajesError.DATOS_INVALIDOS)
        Long ejercicioId,

        @Schema(description = "Solo en OPCION_MULTIPLE: la opción elegida")
        Long opcionId,

        @Schema(description = "Solo en ORDENAR: los ids de todas las opciones, en el orden elegido")
        List<Long> ordenOpcionIds) {
}
