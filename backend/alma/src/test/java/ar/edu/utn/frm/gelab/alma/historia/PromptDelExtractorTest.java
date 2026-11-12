package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Protege {@code historia/prompt-extractor.txt} (contrato del mapa de vida, 2.12): descripción de dos a cuatro
 * oraciones, título corto que nunca es una pregunta, y ejemplos que cumplen lo que el prompt pide. No necesita base de
 * datos ni modelo: la calidad real de la respuesta se prueba a mano con el modelo (contrato del mapa de vida, 6).
 */
class PromptDelExtractorTest {

    private static String prompt;
    private static String plano;
    private static List<JsonNode> ejemplos;

    @BeforeAll
    static void leer() throws IOException {
        try (var entrada = new ClassPathResource("historia/prompt-extractor.txt").getInputStream()) {
            prompt = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        plano = prompt.replaceAll("\\s+", " ");
        JsonMapper mapper = JsonMapper.builder().build();
        ejemplos = prompt.lines().filter(l -> l.startsWith("{\"titulo\"")).map(mapper::readTree).toList();
    }

    private static int oraciones(String texto) {
        return (int) java.util.Arrays.stream(texto.strip().split("(?<=[.!?])\\s+")).filter(s -> !s.isBlank()).count();
    }

    @Test
    void tieneFinesDeLineaLFYNingunRetornoDeCarro() {
        assertThat(prompt).doesNotContain("\r").contains("\n");
    }

    @Test
    void elEsquemaPideDeDosACuatroOracionesSoloConLoQueLaPersonaContoYYaNoUnaODos() {
        assertThat(plano).contains("\"descripcion\": de dos a cuatro oraciones, en tercera persona, solo con lo que la persona contó");
        assertThat(plano).doesNotContain("una o dos oraciones");
    }

    @Test
    void elTituloEsCortoYNuncaUnaPreguntaNiEmpiezaConSignoDeApertura() {
        assertThat(plano).contains("\"titulo\": texto corto que nombra el recuerdo; nunca es una pregunta ni empieza con \"¿\"");
        assertThat(plano).contains("El título nombra el recuerdo")
                .contains("Nunca es una pregunta, no empieza con \"¿\" y no repite la pregunta que recibiste");
    }

    @Test
    void unaRespuestaMuyCortaPuedeTenerUnaSolaOracionYNuncaSeRellena() {
        assertThat(plano).contains("la descripción puede tener una sola oración")
                .contains("nunca la rellenes con detalles que la persona no dijo");
    }

    @Test
    void siguenLasReglasDeNoInventar() {
        assertThat(plano).contains("1. No inventás nada. Usás solo lo que está escrito en la respuesta.")
                .contains("no agregues interpretaciones, emociones, causas ni detalles que no estén en la respuesta");
    }

    @Test
    void elEsquemaDelJsonNoCambio() {
        for (String campo : List.of("\"titulo\"", "\"descripcion\"", "\"anio\"", "\"anioAproximado\"",
                "\"edadMencionada\"", "\"personas\"", "\"lugares\"")) {
            assertThat(prompt).as("campo " + campo).contains(campo);
        }
    }

    @Test
    void losEjemplosSonJsonValidoConElEsquemaDelContrato() {
        assertThat(ejemplos).hasSizeGreaterThanOrEqualTo(3);
        for (JsonNode ejemplo : ejemplos) {
            assertThat(ejemplo.propertyNames()).containsExactlyInAnyOrder("titulo", "descripcion", "anio",
                    "anioAproximado", "edadMencionada", "personas", "lugares");
            assertThat(ejemplo.get("personas").isArray()).isTrue();
            assertThat(ejemplo.get("lugares").isArray()).isTrue();
        }
    }

    @Test
    void ningunTituloDeLosEjemplosEsUnaPregunta() {
        for (JsonNode ejemplo : ejemplos) {
            String titulo = ejemplo.get("titulo").asString();
            assertThat(titulo).isNotBlank().doesNotStartWith("¿").doesNotEndWith("?");
            assertThat(titulo.split("\\s+").length).as("título corto: " + titulo).isLessThanOrEqualTo(8);
        }
    }

    @Test
    void lasDescripcionesDeLosEjemplosTienenDeUnaACuatroOracionesYLosQueCuentanMasTienenAlMenosDos() {
        int conDosOMas = 0;
        for (JsonNode ejemplo : ejemplos) {
            int cantidad = oraciones(ejemplo.get("descripcion").asString());
            assertThat(cantidad).as(ejemplo.get("titulo").asString()).isBetween(1, 4);
            if (cantidad >= 2) {
                conDosOMas++;
            }
        }
        assertThat(conDosOMas).as("ejemplos con descripción de 2 a 4 oraciones").isGreaterThanOrEqualTo(3);
    }

    @Test
    void laRegla12DiceQueLoQueNoRecuerdaNoSeAnotaNiEnLaDescripcion() {
        assertThat(plano).contains("12. Si la persona dice que no se acuerda de algo")
                .contains("no lo anotes como dato ni lo escribas en la descripción")
                .contains("describí solo lo que sí contó")
                .contains("No escribas \"no se acuerda de…\" ni \"no recuerda…\"");
        assertThat(prompt).doesNotContain("\n13. ");
    }

    @Test
    void siNoDijoLaRelacionElVinculoEsNull() {
        assertThat(plano).contains("Si no dijo la relación, \"vinculo\" es null");
    }

    @Test
    void elEjemploDeRespuestaMixtaCreaElRecuerdoSoloConLoQueSiContoYSinAnotarLoQueNoRecuerda() {
        JsonNode mixto = ejemplos.stream()
                .filter(e -> e.get("titulo").asString().equals("Los tiempos con Marta")).findFirst().orElseThrow();

        assertThat(prompt).contains("Respuesta: No me acuerdo del año, pero iba con Marta.");
        assertThat(mixto.get("anio").isNull()).isTrue();
        assertThat(mixto.get("descripcion").asString()).isEqualTo("Pasaba el tiempo con Marta.")
                .doesNotContainIgnoringCase("acuerda").doesNotContainIgnoringCase("recuerda");
        assertThat(mixto.get("personas").get(0).get("nombre").asString()).isEqualTo("Marta");
        assertThat(mixto.get("personas").get(0).get("vinculo").isNull()).isTrue();
    }

    @Test
    void ningunaDescripcionDeLosEjemplosDiceQueLaPersonaNoSeAcuerda() {
        for (JsonNode ejemplo : ejemplos) {
            assertThat(ejemplo.get("descripcion").asString().toLowerCase())
                    .as(ejemplo.get("titulo").asString()).doesNotContain("no se acuerda").doesNotContain("no recuerda");
        }
    }

    @Test
    void hayUnEjemploDeRespuestaCortaConDescripcionDeUnaOracionSinRelleno() {
        JsonNode corto = ejemplos.stream().filter(e -> oraciones(e.get("descripcion").asString()) == 1).findFirst()
                .orElseThrow();
        assertThat(corto.get("anio").isNull()).isTrue();
        assertThat(corto.get("lugares")).isEmpty();
    }
}
