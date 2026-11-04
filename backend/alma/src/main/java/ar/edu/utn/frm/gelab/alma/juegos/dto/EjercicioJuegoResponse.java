package ar.edu.utn.frm.gelab.alma.juegos.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;

/**
 * Ejercicio para jugar: sin explicación, sin nivel y sin respuestas correctas (contrato de juegos, 6.1).
 * textoAMemorizar solo viene en MEMORIA.
 */
public record EjercicioJuegoResponse(
        Long id,
        CategoriaEjercicio categoria,
        FormatoEjercicio formato,
        String consigna,
        String textoAMemorizar,
        List<OpcionJuegoResponse> opciones) {

    /** opcionesMezcladas ya vienen en el orden en que se muestran. */
    public static EjercicioJuegoResponse de(Ejercicio ejercicio, List<OpcionRespuesta> opcionesMezcladas) {
        return new EjercicioJuegoResponse(ejercicio.getId(), ejercicio.getCategoria(), ejercicio.getFormato(),
                ejercicio.getConsigna(),
                ejercicio.getCategoria() == CategoriaEjercicio.MEMORIA ? ejercicio.getTextoAMemorizar() : null,
                opcionesMezcladas.stream().map(OpcionJuegoResponse::de).toList());
    }
}
