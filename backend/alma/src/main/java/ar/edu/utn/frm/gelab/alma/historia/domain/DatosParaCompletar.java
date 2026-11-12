package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.util.List;

/**
 * Lo que una repregunta aporta a un recuerdo ya creado (contrato del grafo, 5.7), ya decidido: solo se usa lo del
 * {@code dato} preguntado. {@code anio}, {@code precision} y {@code etapa} son nulos juntos si la respuesta no
 * aportó un año ni una edad.
 */
public record DatosParaCompletar(DatoFaltante dato, Integer anio, PrecisionAnio precision, EtapaDeVida etapa,
        List<NuevoRecuerdo.PersonaNueva> personas, List<String> lugares) {
}
