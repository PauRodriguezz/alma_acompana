package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LimpiadorDeRespuestaTest {

    @Test
    void quitaEspaciosDeLosExtremos() {
        assertEquals("Hola", LimpiadorDeRespuesta.limpiar("  \n Hola \t"));
    }

    @Test
    void quitaElBloqueDeRazonamiento() {
        assertEquals("Respuesta final", LimpiadorDeRespuesta.limpiar("<think>razono\ncon saltos</think>\n\nRespuesta final "));
    }

    @Test
    void quitaElRazonamientoSiFaltaLaApertura() {
        assertEquals("Respuesta", LimpiadorDeRespuesta.limpiar("razono sin apertura</think>Respuesta"));
    }

    @Test
    void unBloqueSinCerrarNoEsUnaRespuesta() {
        assertEquals("", LimpiadorDeRespuesta.limpiar("<think>nunca termina"));
        assertEquals("Texto", LimpiadorDeRespuesta.limpiar("Texto <think>nunca termina"));
    }

    @Test
    void nuloOVacioQuedaVacio() {
        assertEquals("", LimpiadorDeRespuesta.limpiar(null));
        assertEquals("", LimpiadorDeRespuesta.limpiar("   "));
    }
}
