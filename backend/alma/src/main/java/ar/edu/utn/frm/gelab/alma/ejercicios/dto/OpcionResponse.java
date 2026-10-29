package ar.edu.utn.frm.gelab.alma.ejercicios.dto;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;

public record OpcionResponse(
        Long id,
        String texto,
        Boolean esCorrecta,
        String imagen,
        Integer ordenCorrecto) {

    public static OpcionResponse de(OpcionRespuesta opcion) {
        return new OpcionResponse(opcion.getId(), opcion.getTexto(), opcion.getEsCorrecta(),
                opcion.getImagen(), opcion.getOrdenCorrecto());
    }
}
