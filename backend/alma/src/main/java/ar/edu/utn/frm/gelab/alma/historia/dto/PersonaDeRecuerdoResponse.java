package ar.edu.utn.frm.gelab.alma.historia.dto;

/** Una persona de un recuerdo (contrato de historia de vida, 7.4). {@code vinculo} (y {@code nombre}) pueden ser nulos. */
public record PersonaDeRecuerdoResponse(String id, String nombre, String vinculo) {
}
