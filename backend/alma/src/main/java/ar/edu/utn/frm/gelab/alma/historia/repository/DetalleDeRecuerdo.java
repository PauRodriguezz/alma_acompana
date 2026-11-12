package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.util.List;

/**
 * Un recuerdo con sus personas y lugares (consulta P12). {@code preguntaTexto} es el texto de la pregunta del guion de
 * la que surgió (nulo si no se encuentra).
 */
public record DetalleDeRecuerdo(RecuerdoLeido recuerdo, String preguntaTexto, List<PersonaLeida> personas,
        List<LugarLeido> lugares) {

    /** Una persona del recuerdo. {@code nombre} es nulo si solo se conoce el vínculo; {@code vinculo} puede serlo. */
    public record PersonaLeida(String id, String nombre, String vinculo) {
    }

    public record LugarLeido(String id, String nombre) {
    }
}
