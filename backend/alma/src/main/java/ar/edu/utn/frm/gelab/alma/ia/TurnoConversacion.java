package ar.edu.utn.frm.gelab.alma.ia;

/**
 * Un turno de la conversación que se le manda al modelo. Es propio de {@code ia/}: el módulo de chat lo
 * arma a partir de sus DTOs, así {@code ia/} no depende de {@code chat/}.
 */
public record TurnoConversacion(Rol rol, String texto) {

    public enum Rol {
        PERSONA,
        ALMA
    }
}
