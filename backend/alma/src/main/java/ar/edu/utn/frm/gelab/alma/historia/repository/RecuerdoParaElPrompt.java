package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Un recuerdo con lo necesario para escribir su línea en el prompt del chat (consultas P13 a P15): título,
 * descripción, año, fecha de registro (para ordenar), personas con su vínculo y nombres de lugares.
 */
public record RecuerdoParaElPrompt(String id, String titulo, String descripcion, Integer anio,
        ZonedDateTime fechaRegistro, List<Participante> personas, List<String> lugares) {

    /** {@code nombre} y {@code vinculo} pueden ser nulos (no los dos a la vez en la práctica). */
    public record Participante(String nombre, String vinculo) {
    }
}
