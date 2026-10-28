package ar.edu.utn.frm.gelab.alma.microcapsulas.dto;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;

/**
 * Cuerpo de POST y PUT (contrato del panel, sección 3.3). Las reglas las valida MicrocapsulaService,
 * para devolver los textos exactos de la sección 4.
 */
public record MicrocapsulaRequest(
        String titulo,
        String texto,
        CategoriaMicrocapsula categoria,
        String imagen,
        LocalDate fechaPublicacion) {
}
