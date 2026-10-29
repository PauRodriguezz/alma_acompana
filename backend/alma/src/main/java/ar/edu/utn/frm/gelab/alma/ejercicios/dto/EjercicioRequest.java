package ar.edu.utn.frm.gelab.alma.ejercicios.dto;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * Cuerpo de POST y PUT (contrato del panel, sección 3.2). Las reglas las valida EjercicioService,
 * para devolver los textos exactos de la sección 4.
 */
public record EjercicioRequest(
        CategoriaEjercicio categoria,
        FormatoEjercicio formato,
        String consigna,
        NivelDificultad nivelDificultad,
        String explicacion,
        String textoAMemorizar,
        List<OpcionRequest> opciones) {
}
