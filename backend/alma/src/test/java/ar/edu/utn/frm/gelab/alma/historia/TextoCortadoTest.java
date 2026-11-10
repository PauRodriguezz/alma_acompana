package ar.edu.utn.frm.gelab.alma.historia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.historia.domain.TextoCortado;

/** Corte de textos con "…" (contrato de historia de vida, 7.3 y 5.5.2). Sin base de datos. */
class TextoCortadoTest {

    @Test
    void unTextoCortoQuedaIgual() {
        assertEquals("Hola", TextoCortado.cortar("  Hola ", 140));
    }

    @Test
    void unTextoDeExactamenteElMaximoNoSeCorta() {
        String texto = "a".repeat(140);
        assertEquals(texto, TextoCortado.cortar(texto, 140));
    }

    @Test
    void unTextoLargoSeCortaYTerminaEnPuntosSuspensivos() {
        String cortado = TextoCortado.cortar("a".repeat(200), 140);
        assertEquals("a".repeat(140) + "…", cortado);
    }

    @Test
    void sinEspaciosAntesDeLosPuntosSuspensivos() {
        // El corte cae justo después de un espacio: no queda "casa …".
        assertEquals("casa…", TextoCortado.cortar("casa " + "b".repeat(20), 5));
    }

    @Test
    void nuloDaVacio() {
        assertEquals("", TextoCortado.cortar(null, 10));
    }
}
