package ar.edu.utn.frm.gelab.alma.common;

/**
 * Un error de validación de un campo, dentro de la lista "errores" del 400.
 */
public record ErrorCampo(String campo, String mensaje) {
}
