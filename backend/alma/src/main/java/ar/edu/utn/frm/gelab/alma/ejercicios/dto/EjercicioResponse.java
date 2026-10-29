package ar.edu.utn.frm.gelab.alma.ejercicios.dto;

import java.time.LocalDate;
import java.util.List;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * Detalle de un ejercicio, con sus opciones.
 */
public record EjercicioResponse(
        Long id,
        CategoriaEjercicio categoria,
        FormatoEjercicio formato,
        String consigna,
        NivelDificultad nivelDificultad,
        String explicacion,
        String textoAMemorizar,
        LocalDate fechaAlta,
        List<OpcionResponse> opciones) {

    public static EjercicioResponse de(Ejercicio ejercicio) {
        return new EjercicioResponse(ejercicio.getId(), ejercicio.getCategoria(), ejercicio.getFormato(),
                ejercicio.getConsigna(), ejercicio.getNivelDificultad(), ejercicio.getExplicacion(),
                ejercicio.getTextoAMemorizar(), ejercicio.getFechaAlta(),
                ejercicio.getOpciones().stream().map(OpcionResponse::de).toList());
    }
}
