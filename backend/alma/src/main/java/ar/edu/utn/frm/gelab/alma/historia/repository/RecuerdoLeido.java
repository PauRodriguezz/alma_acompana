package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.time.ZonedDateTime;

/**
 * Un recuerdo de la persona para la línea de tiempo (consulta P10). {@code anio}, {@code precision} y
 * {@code etapaDeVida} son nulos si el recuerdo quedó sin año. {@code paraRepreguntar} trae, en la misma lectura, lo
 * necesario para calcular {@code faltantes} (contrato del mapa de vida, 3.1 y 3.2), sin consultas por recuerdo.
 */
public record RecuerdoLeido(String id, String titulo, String descripcion, Integer anio, String precision,
        String etapaDeVida, ZonedDateTime fechaRegistro, RecuerdoParaRepreguntar paraRepreguntar) {
}
