package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.util.List;

/**
 * El esquema de la extracción (contrato del grafo, 5.5), ya normalizado: {@code anio} y
 * {@code edadMencionada} nulos si no hay dato, las listas nunca son nulas.
 */
public record RespuestaExtraida(String titulo, String descripcion, Integer anio, boolean anioAproximado,
        Integer edadMencionada, List<PersonaExtraida> personas, List<String> lugares) {
}
