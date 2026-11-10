package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.time.ZonedDateTime;

/**
 * Un recuerdo de la persona para la línea de tiempo (consulta P10). {@code anio}, {@code precision} y
 * {@code etapaDeVida} son nulos si el recuerdo quedó sin año.
 */
public record RecuerdoLeido(String id, String titulo, String descripcion, Integer anio, String precision,
        String etapaDeVida, ZonedDateTime fechaRegistro) {
}
