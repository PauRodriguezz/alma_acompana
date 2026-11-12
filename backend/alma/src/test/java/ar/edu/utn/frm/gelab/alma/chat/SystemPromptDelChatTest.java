package ar.edu.utn.frm.gelab.alma.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Protege {@code chat/system-prompt.txt} (contrato del chat, anexo A): primera línea, marcadores, fines de línea LF y
 * la regla 7 sobre las preguntas del guion de historia de vida. La regla 7 acota lo que Alma no debe hacer (formular
 * o proponer preguntas del guion cuando la persona pide retomar su historia) y NO prohíbe las preguntas de
 * seguimiento sobre un recuerdo que la persona cuenta por su cuenta. No necesita base de datos.
 */
class SystemPromptDelChatTest {

    private static String prompt;
    /** El mismo texto con los saltos de línea y los espacios repetidos reducidos a un espacio. */
    private static String plano;

    @BeforeAll
    static void leer() throws IOException {
        try (var entrada = new ClassPathResource("chat/system-prompt.txt").getInputStream()) {
            prompt = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        plano = prompt.replaceAll("\\s+", " ");
    }

    /** Texto de la regla 7: desde "7. " hasta el título EJEMPLOS, en una sola línea. */
    private static String regla7() {
        int desde = prompt.indexOf("\n7. ");
        int hasta = prompt.indexOf("\nEJEMPLOS");
        assertThat(desde).as("existe la regla 7").isPositive();
        assertThat(hasta).as("la regla 7 está antes de EJEMPLOS").isGreaterThan(desde);
        return prompt.substring(desde, hasta).replaceAll("\\s+", " ").strip();
    }

    // --- El archivo ---

    @Test
    void empiezaConLaMismaPrimeraLineaDeSiempre() {
        assertThat(prompt).startsWith("Sos Alma, una asistente digital que acompaña a personas mayores con charla");
        assertThat(plano).contains("como máximo, una pregunta por mensaje");
    }

    @Test
    void tieneFinesDeLineaLFYNingunRetornoDeCarro() {
        assertThat(prompt).doesNotContain("\r");
        assertThat(prompt).contains("\n");
    }

    @Test
    void conservaLosMarcadoresQueReemplazaElBack() {
        for (String marcador : List.of("{respuesta_urgencia}", "{respuesta_malestar_grave}", "{animo}",
                "{recordatorios}", "{citas_proximas}", "{progreso}", "{recuerdos}")) {
            assertThat(prompt).as("marcador " + marcador).contains(marcador);
        }
    }

    @Test
    void lasReglasVanDeLaUnaALaSieteEnOrden() {
        int anterior = prompt.indexOf("REGLAS");
        assertThat(anterior).isPositive();
        for (int numero = 1; numero <= 7; numero++) {
            int posicion = prompt.indexOf("\n" + numero + ". ", anterior);
            assertThat(posicion).as("regla " + numero + " después de la anterior").isGreaterThan(anterior);
            anterior = posicion;
        }
        assertThat(prompt).doesNotContain("\n8. ");
    }

    // --- La regla 7 ---

    @Test
    void laRegla7DiceQueLasPreguntasDelGuionLasHaceLaAplicacion() {
        assertThat(regla7()).contains("Las preguntas del guion de la historia de vida no las formulás ni las "
                + "proponés vos: las hace la aplicación");
    }

    @Test
    void laRegla7ManadaALaSeccionHistoriaDeVidaCuandoLaPersonaPideRetomarla() {
        assertThat(regla7())
                .contains("Cuando la persona te pide seguir o retomar su historia de vida")
                .contains("le decís con calidez que la retome desde la sección Historia de vida")
                .contains("aunque la regla 4 diga que lo preguntes");
    }

    @Test
    void laRegla7NoProhibeLasPreguntasDeSeguimientoSobreUnRecuerdoQueLaPersonaCuenta() {
        String regla = regla7();

        // Lo permite de forma expresa...
        assertThat(regla).contains("Si la persona cuenta un recuerdo por su cuenta, conversás con naturalidad, "
                + "también con preguntas de seguimiento sobre lo que acaba de contar");
        // ...y toda prohibición de preguntas está acotada a las del guion.
        Matcher prohibicion = Pattern.compile("(?:ninguna|ni una|cualquier) pregunta(?<resto>[^.:;]{0,40})")
                .matcher(regla);
        int encontradas = 0;
        while (prohibicion.find()) {
            encontradas++;
            assertThat(prohibicion.group("resto")).as("la prohibición dice de qué preguntas habla: " + prohibicion.group())
                    .startsWith(" del guion");
        }
        assertThat(encontradas).as("la regla tiene su prohibición acotada").isPositive();
        // No hay una prohibición general sobre el pasado ni sobre las preguntas en general.
        assertThat(regla).doesNotContain("pregunta sobre su pasado").doesNotContain("no hagas preguntas")
                .doesNotContain("no hacés preguntas").doesNotContain("sin preguntas");
    }

    @Test
    void laRegla4SigueIgualYElMaximoDeUnaPreguntaPorMensajeTambien() {
        assertThat(plano).contains("Si no sabés algo de la persona, no lo inventás: lo preguntás.");
        assertThat(plano).contains("como máximo, una pregunta por mensaje");
    }

    // --- El ejemplo ---

    @Test
    void tieneElEjemploDeRetomarLaHistoriaDeVidaYAlmaNoPreguntaNada() {
        assertThat(prompt).contains("Persona: Bueno, dale, sigamos con mi historia de vida.");
        int desde = prompt.indexOf("Persona: Bueno, dale, sigamos con mi historia de vida.");
        int hasta = prompt.indexOf("INFORMACIÓN DE LA PERSONA");
        assertThat(hasta).isGreaterThan(desde);
        String respuesta = prompt.substring(desde, hasta).replaceAll("\\s+", " ").strip();
        assertThat(respuesta).contains("Alma: Con gusto. Podés retomarla desde la sección Historia de vida")
                .doesNotContain("?");
    }

    @Test
    void elEjemploVaDespuesDeLosTresEjemplosDeSiempre() {
        int ultimoDeSiempre = prompt.indexOf("¿Hablaste hoy con alguien de tu familia?");
        int nuevo = prompt.indexOf("Persona: Bueno, dale, sigamos con mi historia de vida.");
        int ejemplos = prompt.indexOf("EJEMPLOS");
        assertThat(ejemplos).isPositive();
        assertThat(ultimoDeSiempre).isGreaterThan(ejemplos);
        assertThat(nuevo).isGreaterThan(ultimoDeSiempre);
    }
}
