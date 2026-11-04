package ar.edu.utn.frm.gelab.alma.juegos.dto;

import java.util.List;

/**
 * Resultado de una respuesta (contrato de juegos, 6.2). respuestaCorrecta tiene un texto en opción múltiple
 * y los pasos en el orden correcto en ordenar. explicacion puede ser null.
 */
public record ResultadoResponse(
        boolean acierto,
        String explicacion,
        List<String> respuestaCorrecta,
        boolean rondaCompleta,
        long jugadosHoy) {
}
