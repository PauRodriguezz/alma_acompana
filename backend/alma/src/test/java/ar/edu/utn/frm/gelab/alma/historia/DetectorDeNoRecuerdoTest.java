package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ar.edu.utn.frm.gelab.alma.historia.service.DetectorDeNoRecuerdo;

/** Qué respuestas se tratan como "sin contenido" (contrato del grafo, 5.1 y 5.7). Sin Spring ni bases. */
class DetectorDeNoRecuerdoTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "No me acuerdo", "No me acuerdo.", "no me acuerdo", "NO ME ACUERDO!", "  No   me acuerdo  ",
            "No recuerdo", "No lo recuerdo", "No me acordé", "No me acordaba",
            "No sé", "No sé.", "No se", "No lo sé", "Ni idea", "Ni idea, che", "No tengo idea", "No tengo ni idea",
            "No sabría decirte", "No me viene nada", "Me olvidé", "Se me olvidó", "Lo olvidé",
            "No me acuerdo de nada", "No me acuerdo de nada, la verdad", "La verdad, no me acuerdo",
            "La verdad que no sé", "Uy, no me acuerdo", "Ay, perdón, no me acuerdo", "Mmm, no sé", "Eh... no me acuerdo",
            "No me acuerdo mucho", "No me acuerdo bien", "No me acuerdo de eso", "No me acuerdo, lo siento",
            "No me acuerdo y no sé", "No sé nada", "Nada", "Nada de nada", "No me acuerdo, gracias",
            "No me acuerdo, Alma", "no me acuerdo ya"})
    void lasRespuestasQueSoloDicenQueNoSabenNiRecuerdanNoTienenContenido(String respuesta) {
        assertThat(DetectorDeNoRecuerdo.esSinContenido(respuesta)).as(respuesta).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "No quiero contar", "No quiero contar.", "No quiero responder", "No quiero contestar", "No quiero decirlo",
            "Prefiero no contar", "Prefiero no responder", "No quiero hablar de eso", "No quiero contarlo ahora",
            "La verdad, no quiero contar", "No quiero responder, perdón", "No quiero contar nada"})
    void noQuererContarONoQuererResponderTampocoTieneContenido(String respuesta) {
        assertThat(DetectorDeNoRecuerdo.esSinContenido(respuesta)).as(respuesta).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "No me acuerdo del año, pero iba con Marta", "No me acuerdo del año, pero iba con Marta.",
            "No me acuerdo de nada, solo que mi mamá cocinaba", "No sé el año, pero fue en Allen",
            "No me acuerdo cómo se llamaba, pero era alta", "No quiero contar el año, pero era con mi hermana",
            "No me acuerdo del año", "No me acuerdo de su nombre", "No sé cuándo fue",
            "Nunca me acordé de ella pero la quería mucho", "Iba con Marta", "Con mi abuela Rosa",
            "Jugábamos a la rayuela", "Fue en 1962", "En Allen", "Ninguna", "Nadie", "Mamá",
            "No hay nada que contar sobre mi escuela, era chica y vivía lejos"})
    void unaRespuestaConContenidoNoSeTrataComoSinContenido(String respuesta) {
        assertThat(DetectorDeNoRecuerdo.esSinContenido(respuesta)).as(respuesta).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "...", "?!" })
    void unTextoNuloVacioOSoloConSignosNoSeConsideraSinContenido(String respuesta) {
        assertThat(DetectorDeNoRecuerdo.esSinContenido(respuesta)).isFalse();
    }

    @Test
    void soloSeEvaluanRespuestasCortas() {
        String larga = "no me acuerdo ".repeat(10);

        assertThat(larga.strip().length()).as("más de 60 caracteres").isGreaterThan(60);
        assertThat(DetectorDeNoRecuerdo.esSinContenido(larga)).isFalse();
        assertThat(DetectorDeNoRecuerdo.esSinContenido("no me acuerdo ".repeat(3))).isTrue();
    }

    @Test
    void sinTildesDiacriticosNiMayusculasDaLoMismo() {
        assertThat(DetectorDeNoRecuerdo.esSinContenido("NO ME ACORDÉ")).isTrue();
        assertThat(DetectorDeNoRecuerdo.esSinContenido("no me acorde")).isTrue();
        assertThat(DetectorDeNoRecuerdo.esSinContenido("¡No sé!")).isTrue();
    }
}
