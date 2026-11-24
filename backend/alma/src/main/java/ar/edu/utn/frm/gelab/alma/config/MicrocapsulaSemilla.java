package ar.edu.utn.frm.gelab.alma.config;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;

/**
 * Una microcápsula del seed: el título, la categoría y la fecha están acá, y el texto vive en un archivo de
 * {@code src/main/resources/microcapsulas/} (ver {@link TextosDeMicrocapsulas}).
 */
public record MicrocapsulaSemilla(
        String titulo,
        CategoriaMicrocapsula categoria,
        LocalDate fechaPublicacion,
        String archivo) {
}
