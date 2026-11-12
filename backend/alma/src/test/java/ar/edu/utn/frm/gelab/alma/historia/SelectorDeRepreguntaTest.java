package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.ANIO;
import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.LUGARES;
import static ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante.PERSONAS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaRepreguntar;
import ar.edu.utn.frm.gelab.alma.historia.service.SelectorDeRepregunta;
import ar.edu.utn.frm.gelab.alma.historia.service.SelectorDeRepregunta.Candidata;

/** La elección de la repregunta candidata (contrato del grafo, 5.7 y 7.5), sin bases. */
class SelectorDeRepreguntaTest {

    private static final ZonedDateTime BASE = ZonedDateTime.parse("2026-11-09T10:00:00-03:00");
    private static final int MAX = 2;

    /** Un recuerdo al que le faltan los datos indicados y con los intentos indicados (anio, lugares, personas). */
    private static RecuerdoParaRepreguntar recuerdo(String id, int minutos, List<DatoFaltante> lista,
            Set<DatoFaltante> faltan, int... intentos) {
        int ia = intentos.length > 0 ? intentos[0] : 0;
        int il = intentos.length > 1 ? intentos[1] : 0;
        int ip = intentos.length > 2 ? intentos[2] : 0;
        return new RecuerdoParaRepreguntar(id, BASE.plusMinutes(minutos), lista, faltan.contains(ANIO),
                faltan.contains(LUGARES), faltan.contains(PERSONAS), ia, il, ip, "Recuerdo " + id);
    }

    private static Candidata elegir(List<RecuerdoParaRepreguntar> recuerdos, Set<DatoFaltante> excluir) {
        return SelectorDeRepregunta.siguiente(recuerdos, excluir, MAX).orElse(null);
    }

    private static final List<DatoFaltante> TODOS = List.of(ANIO, LUGARES, PERSONAS);
    private static final Set<DatoFaltante> FALTA_TODO = Set.of(ANIO, LUGARES, PERSONAS);

    @Test
    void sinRecuerdosNoHayCandidata() {
        assertThat(elegir(List.of(), Set.of())).isNull();
    }

    @Test
    void elOrdenDentroDeUnRecuerdoEsElDeLaListaDeLaPregunta() {
        assertThat(elegir(List.of(recuerdo("r", 0, TODOS, FALTA_TODO)), Set.of())).isEqualTo(new Candidata("r", ANIO));
        assertThat(elegir(List.of(recuerdo("r", 0, List.of(PERSONAS, ANIO), FALTA_TODO)), Set.of()))
                .isEqualTo(new Candidata("r", PERSONAS));
    }

    @Test
    void soloSeOfreceUnDatoDeLaListaDeLaPregunta() {
        // Falta todo, pero la lista es solo LUGARES.
        assertThat(elegir(List.of(recuerdo("r", 0, List.of(LUGARES), FALTA_TODO)), Set.of()))
                .isEqualTo(new Candidata("r", LUGARES));
        // Lista vacía (por ejemplo, "¿Dónde naciste?"): nunca hay repregunta.
        assertThat(elegir(List.of(recuerdo("r", 0, List.of(), FALTA_TODO)), Set.of())).isNull();
    }

    @Test
    void soloSeOfreceLoQueFalta() {
        assertThat(elegir(List.of(recuerdo("r", 0, TODOS, Set.of(PERSONAS))), Set.of()))
                .isEqualTo(new Candidata("r", PERSONAS));
        assertThat(elegir(List.of(recuerdo("r", 0, TODOS, Set.of())), Set.of())).isNull();
    }

    @Test
    void excluirSacaEsosDatos() {
        List<RecuerdoParaRepreguntar> uno = List.of(recuerdo("r", 0, TODOS, FALTA_TODO));
        assertThat(elegir(uno, Set.of(ANIO))).isEqualTo(new Candidata("r", LUGARES));
        assertThat(elegir(uno, Set.of(ANIO, LUGARES))).isEqualTo(new Candidata("r", PERSONAS));
        assertThat(elegir(uno, Set.of(ANIO, LUGARES, PERSONAS))).isNull();
    }

    @Test
    void conElContadorEnElMaximoYaNoSeOfrece() {
        // intentos: anio 2, lugares 1, personas 0.
        List<RecuerdoParaRepreguntar> uno = List.of(recuerdo("r", 0, TODOS, FALTA_TODO, 2, 1, 0));
        assertThat(elegir(uno, Set.of())).isEqualTo(new Candidata("r", LUGARES));
        assertThat(elegir(List.of(recuerdo("r", 0, TODOS, FALTA_TODO, 2, 2, 2)), Set.of())).isNull();
        // Con 1 intento todavía se ofrece (el máximo es 2 en total).
        assertThat(elegir(List.of(recuerdo("r", 0, TODOS, FALTA_TODO, 1, 0, 0)), Set.of()))
                .isEqualTo(new Candidata("r", ANIO));
    }

    @Test
    void sinPreguntaIdGanaElRecuerdoMasAntiguoQueTengaUnaCandidata() {
        RecuerdoParaRepreguntar viejoSinCandidata = recuerdo("viejo", 0, TODOS, Set.of());
        RecuerdoParaRepreguntar medio = recuerdo("medio", 10, TODOS, Set.of(LUGARES));
        RecuerdoParaRepreguntar nuevo = recuerdo("nuevo", 20, TODOS, FALTA_TODO);

        // Se pasan desordenados: el selector no depende del orden de entrada.
        assertThat(elegir(List.of(nuevo, medio, viejoSinCandidata), Set.of())).isEqualTo(new Candidata("medio", LUGARES));
        assertThat(elegir(List.of(nuevo, medio), Set.of(LUGARES))).isEqualTo(new Candidata("nuevo", ANIO));
    }

    // ---------- faltantes (contrato del mapa de vida, 3.1 y 3.2) ----------

    private static List<DatoFaltante> faltantes(RecuerdoParaRepreguntar recuerdo) {
        return SelectorDeRepregunta.faltantes(recuerdo, MAX);
    }

    @Test
    void faltantesListaLosDatosQueFaltanEnElOrdenDeLaPregunta() {
        assertThat(faltantes(recuerdo("r", 0, TODOS, FALTA_TODO))).containsExactly(ANIO, LUGARES, PERSONAS);
        assertThat(faltantes(recuerdo("r", 0, List.of(PERSONAS, ANIO, LUGARES), FALTA_TODO)))
                .containsExactly(PERSONAS, ANIO, LUGARES);
    }

    @Test
    void faltantesSoloTraeLoQueFaltaHoy() {
        // Sin año (o con año ESTIMADA) falta ANIO; sin lugares, LUGARES; sin personas, PERSONAS.
        assertThat(faltantes(recuerdo("r", 0, TODOS, Set.of(ANIO)))).containsExactly(ANIO);
        assertThat(faltantes(recuerdo("r", 0, TODOS, Set.of(LUGARES)))).containsExactly(LUGARES);
        assertThat(faltantes(recuerdo("r", 0, TODOS, Set.of(PERSONAS)))).containsExactly(PERSONAS);
        assertThat(faltantes(recuerdo("r", 0, TODOS, Set.of(ANIO, PERSONAS)))).containsExactly(ANIO, PERSONAS);
        assertThat(faltantes(recuerdo("r", 0, TODOS, Set.of()))).isEmpty();
    }

    @Test
    void faltantesSacaElDatoConElContadorEnElMaximo() {
        assertThat(faltantes(recuerdo("r", 0, TODOS, FALTA_TODO, 2, 1, 0))).containsExactly(LUGARES, PERSONAS);
        assertThat(faltantes(recuerdo("r", 0, TODOS, FALTA_TODO, 1, 1, 1))).containsExactly(ANIO, LUGARES, PERSONAS);
        assertThat(faltantes(recuerdo("r", 0, TODOS, FALTA_TODO, 2, 2, 2))).isEmpty();
    }

    @Test
    void faltantesSoloTraeLosDatosDeLaListaDeLaPreguntaDeOrigen() {
        assertThat(faltantes(recuerdo("r", 0, List.of(LUGARES), FALTA_TODO))).containsExactly(LUGARES);
        assertThat(faltantes(recuerdo("r", 0, List.of(), FALTA_TODO))).isEmpty();
    }

    @Test
    void siguienteYFaltantesUsanLaMismaReglaDeCandidato() {
        RecuerdoParaRepreguntar r = recuerdo("r", 0, TODOS, FALTA_TODO, 2, 0, 0);
        assertThat(elegir(List.of(r), Set.of()).dato()).isEqualTo(faltantes(r).get(0));
    }

    @Test
    void siElMasAntiguoTieneCandidataEsElElegido() {
        RecuerdoParaRepreguntar viejo = recuerdo("viejo", 0, TODOS, FALTA_TODO);
        RecuerdoParaRepreguntar nuevo = recuerdo("nuevo", 5, TODOS, FALTA_TODO);
        assertThat(elegir(List.of(nuevo, viejo), Set.of())).isEqualTo(new Candidata("viejo", ANIO));
    }
}
