package ar.edu.utn.frm.gelab.alma.ejercicios.dto;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * Fila de la lista del panel.
 */
public record EjercicioResumenResponse(
        Long id,
        CategoriaEjercicio categoria,
        String consigna,
        FormatoEjercicio formato,
        NivelDificultad nivelDificultad) {

    public static EjercicioResumenResponse de(Ejercicio ejercicio) {
        return new EjercicioResumenResponse(ejercicio.getId(), ejercicio.getCategoria(), ejercicio.getConsigna(),
                ejercicio.getFormato(), ejercicio.getNivelDificultad());
    }
}
