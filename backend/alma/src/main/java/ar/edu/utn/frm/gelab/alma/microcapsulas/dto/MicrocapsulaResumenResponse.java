package ar.edu.utn.frm.gelab.alma.microcapsulas.dto;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;

/**
 * Fila de la lista del panel.
 */
public record MicrocapsulaResumenResponse(
        Long id,
        String titulo,
        CategoriaMicrocapsula categoria,
        LocalDate fechaPublicacion,
        String imagen) {

    public static MicrocapsulaResumenResponse de(Microcapsula microcapsula) {
        return new MicrocapsulaResumenResponse(microcapsula.getId(), microcapsula.getTitulo(),
                microcapsula.getCategoria(), microcapsula.getFechaPublicacion(), microcapsula.getImagen());
    }
}
