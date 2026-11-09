package ar.edu.utn.frm.gelab.alma.historia.extraccion;

/**
 * Lo que devolvió el extractor: la respuesta lista para escribir, cuántos pedidos al modelo hicieron falta y si
 * se usó el respaldo (contrato, 5.4, paso 7).
 */
public record ResultadoDeExtraccion(RespuestaExtraida respuesta, int pedidos, boolean respaldo) {
}
