package ar.edu.utn.frm.gelab.alma.microcapsulas.dto;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;

/**
 * Detalle de una microcápsula.
 */
public record MicrocapsulaResponse(
        Long id,
        String titulo,
        String texto,
        CategoriaMicrocapsula categoria,
        String imagen,
        LocalDate fechaPublicacion) {

    public static MicrocapsulaResponse de(Microcapsula microcapsula) {
        return new MicrocapsulaResponse(microcapsula.getId(), microcapsula.getTitulo(), microcapsula.getTexto(),
                microcapsula.getCategoria(), microcapsula.getImagen(), microcapsula.getFechaPublicacion());
    }
}
