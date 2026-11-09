package ar.edu.utn.frm.gelab.alma.historia.repository;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;

/**
 * Los fragmentos de Cypher de las repreguntas (contrato del grafo, 5.7), escritos una sola vez para que la lectura
 * de 7.5 y el chequeo de 7.6 y 7.7 no se puedan contradecir. Se arman solo a partir del enum: nunca con texto de la
 * persona. En todos, {@code r} es el {@code Recuerdo}.
 */
final class CypherRepreguntas {

    private CypherRepreguntas() {
    }

    /** La propiedad del recuerdo que cuenta cuántas veces se preguntó por ese dato. */
    static String propiedadIntentos(DatoFaltante dato) {
        return switch (dato) {
            case ANIO -> "intentosAnio";
            case LUGARES -> "intentosLugares";
            case PERSONAS -> "intentosPersonas";
        };
    }

    /** Cuándo falta el dato: una estimación por franja no cuenta como año real. */
    static String condicionFalta(DatoFaltante dato) {
        return switch (dato) {
            case ANIO -> "(r.anio IS NULL OR r.precision = 'ESTIMADA')";
            case LUGARES -> "NOT EXISTS { (r)-[:OCURRIO_EN]->() }";
            case PERSONAS -> "NOT EXISTS { (r)<-[:PARTICIPA_EN]-() }";
        };
    }
}
