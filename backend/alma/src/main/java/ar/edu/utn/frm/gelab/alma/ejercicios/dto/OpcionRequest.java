package ar.edu.utn.frm.gelab.alma.ejercicios.dto;

/**
 * Una opción (o un paso, en ORDENAR) dentro del cuerpo de POST y PUT.
 */
public record OpcionRequest(
        String texto,
        Boolean esCorrecta,
        String imagen,
        Integer ordenCorrecto) {
}
