package ar.edu.utn.frm.gelab.alma.historia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.historia.domain.NombreNormalizado;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaElPrompt;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaElPrompt.Participante;
import ar.edu.utn.frm.gelab.alma.historia.service.TextoDeRecuerdos;

/** Coincidencia de nombres y formato de las líneas del prompt (contrato de historia de vida, 5.5). Sin base de datos. */
class TextoDeRecuerdosTest {

    private static boolean menciona(String mensaje, String nombre) {
        return TextoDeRecuerdos.menciona(DetectorDeRiesgo.normalizar(mensaje), nombre);
    }

    private static RecuerdoParaElPrompt recuerdo(Integer anio, String descripcion, List<Participante> personas,
            List<String> lugares) {
        return new RecuerdoParaElPrompt("id", "Mi maestra Elena", descripcion, anio, ZonedDateTime.now(), personas,
                lugares);
    }

    // ---------- Coincidencia ----------

    @Test
    void unNombreConPuntuacionCoincideConLasTresFormasDeEscribirlo() {
        // El nombre guardado conserva la puntuación ("escuela n° 12"); el mensaje normalizado no la tiene.
        String guardado = NombreNormalizado.de("Escuela N° 12");
        assertEquals("escuela n° 12", guardado);

        assertTrue(menciona("Fui a la escuela n 12", guardado));
        assertTrue(menciona("Fui a la Escuela N° 12", guardado));
        assertTrue(menciona("Fui a la Escuela N° 12.", "Escuela N° 12"));
        // "N°12" sin espacio también: el ° no es letra ni número y pasa a un espacio.
        assertTrue(menciona("Fui a la Escuela N°12", "Escuela N° 12"));
    }

    @Test
    void ignoraTildesMayusculasYPuntuacion() {
        assertTrue(menciona("Hoy me acordé de ELENA, ¡qué linda!", "Elena"));
        assertTrue(menciona("Fui a Cipolletti", "CIPOLLETTI"));
        assertTrue(menciona("Fuimos a Neuquén", "Neuquen"));
        assertTrue(menciona("la señora Peña", "Peña"));
    }

    @Test
    void soloCoincidePorPalabraCompleta() {
        assertFalse(menciona("Hablé con Elenita", "Elena"));
        assertFalse(menciona("Marcelena vino", "Elena"));
        assertFalse(menciona("La casa de la costanera", "la costa"));
        assertTrue(menciona("Fuimos a la costa en verano", "la costa"));
        assertTrue(menciona("Elena", "Elena"));
    }

    @Test
    void unNombreVacioONuloNoCoincideConNada() {
        assertFalse(menciona("cualquier cosa", ""));
        assertFalse(menciona("cualquier cosa", "   "));
        assertFalse(menciona("cualquier cosa", "¿?"));
        assertFalse(menciona("cualquier cosa", null));
    }

    // ---------- Progreso ----------

    @Test
    void elProgresoLleva3Cantidades() {
        assertEquals("Respondió 4, salteó 1, quedan 16.", TextoDeRecuerdos.progreso(4, 1, 16));
    }

    // ---------- Líneas ----------

    @Test
    void laLineaCompletaEsLaDelContrato() {
        RecuerdoParaElPrompt r = recuerdo(1957, "Su maestra de primer grado, que le enseñó a leer.",
                List.of(new Participante("Elena", "maestra")), List.of("Escuela N° 12"));

        assertEquals("- Mi maestra Elena (1957): Su maestra de primer grado, que le enseñó a leer. "
                + "Con Elena (maestra), en Escuela N° 12.", TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void siLaDescripcionNoTerminaEnPuntoSeLeAgregaUno() {
        RecuerdoParaElPrompt r = recuerdo(1957, "Su maestra de primer grado", List.of(), List.of());

        assertEquals("- Mi maestra Elena (1957): Su maestra de primer grado.", TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void sinAnioSeOmiteElAnio() {
        RecuerdoParaElPrompt r = recuerdo(null, "Su maestra.", List.of(), List.of());

        assertEquals("- Mi maestra Elena: Su maestra.", TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void sinLugarNiPersonasSeCortaTrasLaDescripcion() {
        RecuerdoParaElPrompt r = recuerdo(1957, "Su maestra.", List.of(), List.of());

        assertEquals("- Mi maestra Elena (1957): Su maestra.", TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void conPersonasYSinLugarOConLugarYSinPersonas() {
        assertEquals("- Mi maestra Elena (1957): Su maestra. Con Elena (maestra).", TextoDeRecuerdos
                .linea(recuerdo(1957, "Su maestra.", List.of(new Participante("Elena", "maestra")), List.of()), 300));
        assertEquals("- Mi maestra Elena (1957): Su maestra. En Escuela N° 12.", TextoDeRecuerdos
                .linea(recuerdo(1957, "Su maestra.", List.of(), List.of("Escuela N° 12")), 300));
    }

    @Test
    void personasSinNombreSinVinculoYVarias() {
        RecuerdoParaElPrompt r = recuerdo(1980, "Un viaje.", List.of(new Participante(null, "marido"),
                new Participante("Elena", null), new Participante(null, null)), List.of("la costa", "Mar del Plata"));

        assertEquals("- Mi maestra Elena (1980): Un viaje. Con marido, Elena, en la costa, Mar del Plata.",
                TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void laDescripcionSeCortaAlMaximoConPuntosSuspensivosSinDuplicarElPunto() {
        RecuerdoParaElPrompt r = recuerdo(1957, "a".repeat(400), List.of(), List.of());

        String linea = TextoDeRecuerdos.linea(r, 300);

        assertEquals("- Mi maestra Elena (1957): " + "a".repeat(300) + "…", linea);
    }

    @Test
    void sinDescripcionConExtrasQuedaTituloYExtras() {
        RecuerdoParaElPrompt r = recuerdo(null, "  ", List.of(new Participante("Elena", "maestra")), List.of());

        assertEquals("- Mi maestra Elena. Con Elena (maestra).", TextoDeRecuerdos.linea(r, 300));
    }

    @Test
    void losSaltosDeLineaNoRompenLaLinea() {
        RecuerdoParaElPrompt r = recuerdo(1957, "Primera parte.\n\nSegunda   parte.", List.of(), List.of());

        assertEquals("- Mi maestra Elena (1957): Primera parte. Segunda parte.", TextoDeRecuerdos.linea(r, 300));
    }
}
