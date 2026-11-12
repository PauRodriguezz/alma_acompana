package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.historia.config.CargaInicialDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import tools.jackson.databind.json.JsonMapper;

/**
 * Los recursos del guion (contrato del grafo, Anexo A y sección 2). No necesita Neo4j: lee
 * {@code historia/guion.json} y {@code historia/restricciones.cypher} del classpath.
 */
class GuionJsonTest {

    private static final Set<String> DATOS = Set.of("ANIO", "LUGARES", "PERSONAS");

    private final CargaInicialDelGrafo carga = new CargaInicialDelGrafo(null, JsonMapper.builder().build(), null,
            null, false);

    @Test
    void elGuionTieneVeintiunaPreguntasConIdsYOrdenesUnicos() {
        List<PreguntaDelGuion> guion = carga.leerGuion();

        assertThat(guion).hasSize(21);
        assertThat(guion.stream().map(PreguntaDelGuion::id)).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, 21).boxed().toList());
        assertThat(guion.stream().map(PreguntaDelGuion::orden).collect(Collectors.toSet())).hasSize(21);
        assertThat(guion).allSatisfy(p -> assertThat(p.texto()).isNotBlank());
    }

    @Test
    void cadaPreguntaTieneSuListaDeRepreguntasValida() {
        for (PreguntaDelGuion p : carga.leerGuion()) {
            assertThat(p.repreguntas()).as("repreguntas de la pregunta %d", p.id()).isNotNull()
                    .hasSizeLessThanOrEqualTo(3).doesNotHaveDuplicates().allMatch(DATOS::contains);
        }
    }

    @Test
    void laFranjaDeEdadTieneLosDosLimitesONinguno() {
        for (PreguntaDelGuion p : carga.leerGuion()) {
            assertThat(p.edadDesde() == null).as("franja de la pregunta %d", p.id()).isEqualTo(p.edadHasta() == null);
            if (p.edadDesde() != null) {
                assertThat(p.edadDesde()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(p.edadHasta());
            }
        }
    }

    @Test
    void algunasPreguntasDelAnexoA() {
        List<PreguntaDelGuion> guion = carga.leerGuion();
        PreguntaDelGuion nacimiento = guion.stream().filter(p -> p.id() == 1).findFirst().orElseThrow();
        PreguntaDelGuion escuela = guion.stream().filter(p -> p.id() == 7).findFirst().orElseThrow();
        PreguntaDelGuion hijos = guion.stream().filter(p -> p.id() == 15).findFirst().orElseThrow();

        // "¿Dónde naciste?" nunca se repregunta, y su franja es la del nacimiento.
        assertThat(nacimiento.repreguntas()).isEmpty();
        assertThat(nacimiento.edadDesde()).isZero();
        assertThat(nacimiento.edadHasta()).isZero();
        assertThat(escuela.repreguntas()).containsExactly("ANIO", "LUGARES", "PERSONAS");
        assertThat(escuela.edadDesde()).isEqualTo(6);
        assertThat(escuela.edadHasta()).isEqualTo(12);
        // Sin franja: no sirve para estimar el año.
        assertThat(hijos.edadDesde()).isNull();
        assertThat(hijos.edadHasta()).isNull();
    }

    @Test
    void lasRestriccionesSonLasSeisSentenciasIdempotentes() {
        List<String> restricciones = carga.leerRestricciones();

        assertThat(restricciones).hasSize(6).allSatisfy(s -> assertThat(s)
                .startsWith("CREATE CONSTRAINT").contains("IF NOT EXISTS").contains("IS UNIQUE"));
    }
}
