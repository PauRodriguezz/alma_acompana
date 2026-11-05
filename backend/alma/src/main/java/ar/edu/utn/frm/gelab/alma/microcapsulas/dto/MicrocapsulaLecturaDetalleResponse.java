package ar.edu.utn.frm.gelab.alma.microcapsulas.dto;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;

/**
 * Detalle de una microcápsula para la persona mayor (contrato de microcápsulas, 6.2). Sin fechas.
 */
public record MicrocapsulaLecturaDetalleResponse(
        Long id,
        String titulo,
        String texto,
        CategoriaMicrocapsula categoria,
        String imagen,
        boolean guardada) {

    public static MicrocapsulaLecturaDetalleResponse de(Microcapsula microcapsula, boolean guardada) {
        return new MicrocapsulaLecturaDetalleResponse(microcapsula.getId(), microcapsula.getTitulo(),
                microcapsula.getTexto(), microcapsula.getCategoria(), microcapsula.getImagen(), guardada);
    }
}
