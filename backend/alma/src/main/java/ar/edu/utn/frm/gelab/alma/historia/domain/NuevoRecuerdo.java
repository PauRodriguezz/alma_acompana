package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.util.List;

/**
 * Lo que hay que escribir en el grafo para un recuerdo nuevo (contrato del grafo, 5.6), ya decidido: año,
 * precisión y etapa calculados, personas y lugares sin repetir. {@code anio}, {@code precision} y
 * {@code etapa} son nulos juntos (recuerdo sin año).
 */
public record NuevoRecuerdo(int preguntaId, String titulo, String descripcion, Integer anio,
        PrecisionAnio precision, EtapaDeVida etapa, List<PersonaNueva> personas, List<String> lugares) {

    /** {@code nombre} es nulo si solo se mencionó el vínculo ("mi tío"). */
    public record PersonaNueva(String nombre, String vinculo) {
    }
}
