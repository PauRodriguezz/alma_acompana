package ar.edu.utn.frm.gelab.alma.microcapsulas.dto;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;

/**
 * Tarjeta de la pantalla Leer (contrato de microcápsulas, 6.1). Sin fechas: la persona no las usa.
 */
public record MicrocapsulaLecturaResumenResponse(
        Long id,
        String titulo,
        CategoriaMicrocapsula categoria,
        String imagen,
        boolean guardada) {

    public static MicrocapsulaLecturaResumenResponse de(Microcapsula microcapsula, boolean guardada) {
        return new MicrocapsulaLecturaResumenResponse(microcapsula.getId(), microcapsula.getTitulo(),
                microcapsula.getCategoria(), microcapsula.getImagen(), guardada);
    }
}
