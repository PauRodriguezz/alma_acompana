package ar.edu.utn.frm.gelab.alma.juegos.dto;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;

/**
 * Opción tal como la ve la persona: sin esCorrecta ni ordenCorrecto (contrato de juegos, 6.1).
 */
public record OpcionJuegoResponse(Long id, String texto, String imagen) {

    public static OpcionJuegoResponse de(OpcionRespuesta opcion) {
        return new OpcionJuegoResponse(opcion.getId(), opcion.getTexto(), opcion.getImagen());
    }
}
