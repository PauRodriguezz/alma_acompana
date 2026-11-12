package ar.edu.utn.frm.gelab.alma.historia.extraccion;

/** Una persona del JSON de la extracción (contrato, 5.5). {@code nombre} es nulo si solo se dijo el vínculo. */
public record PersonaExtraida(String nombre, String vinculo) {
}
