package ar.edu.utn.frm.gelab.alma.historia.extraccion;

/**
 * Lo que devolvió el extractor: la respuesta lista para escribir, cuántos pedidos al modelo hicieron falta, si se
 * usó el respaldo (contrato, 5.4, paso 7) y por qué. El motivo sirve para una repregunta: si el modelo no estaba
 * disponible, se devuelve el intento del dato (5.7).
 */
public record ResultadoDeExtraccion(RespuestaExtraida respuesta, int pedidos, boolean respaldo,
        MotivoDeRespaldo motivo) {

    /** Por qué se usó el respaldo. */
    public enum MotivoDeRespaldo {
        /** No se usó el respaldo. */
        NINGUNO,
        /** El modelo falló, tardó de más, no tuvo turno o no respondió después del reintento. */
        MODELO_NO_DISPONIBLE,
        /** El modelo respondió pero la respuesta no sirvió dos veces, o hubo una falla inesperada. */
        OTRO
    }

    public ResultadoDeExtraccion(RespuestaExtraida respuesta, int pedidos, boolean respaldo) {
        this(respuesta, pedidos, respaldo, respaldo ? MotivoDeRespaldo.OTRO : MotivoDeRespaldo.NINGUNO);
    }

    /** ¿El respaldo se usó porque el modelo no estaba disponible? */
    public boolean porModeloNoDisponible() {
        return respaldo && motivo == MotivoDeRespaldo.MODELO_NO_DISPONIBLE;
    }
}
