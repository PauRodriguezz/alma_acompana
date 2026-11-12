package ar.edu.utn.frm.gelab.alma.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.chat.guardrails.TipoDeRiesgo;

/** {@code Respuestas.para(TipoDeRiesgo)}: el texto predefinido de cada riesgo (lo comparten el chat y la historia). */
class RespuestasParaTest {

    @Test
    void devuelveElTextoQueCorrespondeACadaRiesgo() {
        ConfiguracionChat.Respuestas respuestas = new ConfiguracionChat.Respuestas();
        respuestas.setUrgencia("texto de urgencia");
        respuestas.setMalestarGrave("texto de malestar");

        assertEquals("texto de urgencia", respuestas.para(TipoDeRiesgo.URGENCIA_MEDICA));
        assertEquals("texto de malestar", respuestas.para(TipoDeRiesgo.MALESTAR_GRAVE));
    }
}
