package ar.edu.utn.frm.gelab.alma.config;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * Un ejercicio del seed. En OPCION_MULTIPLE la primera de {@code opciones} es la correcta; en ORDENAR,
 * los pasos van en el orden correcto. {@code textoAMemorizar} es null salvo en MEMORIA.
 */
public record EjercicioSemilla(
        CategoriaEjercicio categoria,
        FormatoEjercicio formato,
        NivelDificultad nivelDificultad,
        String consigna,
        String explicacion,
        String textoAMemorizar,
        List<String> opciones) {
}
