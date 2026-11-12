package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.time.ZonedDateTime;
import java.util.List;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;

/**
 * Lo que hace falta saber de un recuerdo para elegir una repregunta (contrato del grafo, 5.7): los datos por los
 * que su pregunta de origen permite repreguntar (en orden de prioridad), cuáles faltan hoy y cuántas veces se
 * preguntó por cada uno. {@code titulo} es el del recuerdo (contrato del mapa de vida, 3.3); un recuerdo sin pregunta
 * de origen tiene la lista {@code repreguntas} vacía, así que nunca tiene datos candidatos.
 */
public record RecuerdoParaRepreguntar(String id, ZonedDateTime fechaRegistro, List<DatoFaltante> repreguntas,
        boolean faltaAnio, boolean faltaLugares, boolean faltaPersonas, int intentosAnio, int intentosLugares,
        int intentosPersonas, String titulo) {

    public boolean falta(DatoFaltante dato) {
        return switch (dato) {
            case ANIO -> faltaAnio;
            case LUGARES -> faltaLugares;
            case PERSONAS -> faltaPersonas;
        };
    }

    public int intentos(DatoFaltante dato) {
        return switch (dato) {
            case ANIO -> intentosAnio;
            case LUGARES -> intentosLugares;
            case PERSONAS -> intentosPersonas;
        };
    }
}
