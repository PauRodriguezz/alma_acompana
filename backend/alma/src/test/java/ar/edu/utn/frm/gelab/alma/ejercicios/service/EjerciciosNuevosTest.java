package ar.edu.utn.frm.gelab.alma.ejercicios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.config.EjercicioSemilla;
import ar.edu.utn.frm.gelab.alma.config.EjerciciosNuevos;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioRequest;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.OpcionRequest;

/**
 * Los 36 ejercicios nuevos del seed cumplen las reglas del panel, con el mismo validador que usa
 * EjercicioService (largos, cantidad de opciones, una sola correcta, posiciones de ordenar).
 * No usa Spring ni las bases de datos.
 */
class EjerciciosNuevosTest {

    static List<EjercicioSemilla> semillas() {
        return EjerciciosNuevos.TODOS;
    }

    /** Arma el pedido como lo manda el panel: la primera opción es la correcta; en ordenar, posiciones 1..n. */
    private static EjercicioRequest comoPedido(EjercicioSemilla s) {
        boolean ordenar = s.formato() == FormatoEjercicio.ORDENAR;
        List<OpcionRequest> opciones = IntStream.range(0, s.opciones().size())
                .mapToObj(i -> ordenar
                        ? new OpcionRequest(s.opciones().get(i), null, null, i + 1)
                        : new OpcionRequest(s.opciones().get(i), i == 0, null, null))
                .toList();
        return new EjercicioRequest(s.categoria(), s.formato(), s.consigna(), s.nivelDificultad(),
                s.explicacion(), s.textoAMemorizar(), opciones);
    }

    @Test
    void hayTreintaYSeisEjerciciosConClaveUnica() {
        assertThat(semillas()).hasSize(36);
        // La clave con la que el seed decide si un ejercicio ya existe: (categoría, nivel, consigna).
        assertThat(semillas().stream().map(s -> List.of(s.categoria(), s.nivelDificultad(), s.consigna())))
                .doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("semillas")
    void cumpleLasReglasDelValidador(EjercicioSemilla semilla) {
        try {
            EjercicioService.validar(comoPedido(semilla));
        } catch (DatosInvalidosException e) {
            fail("«%s» (%s, %s) no pasa el validador: %s", semilla.consigna(), semilla.categoria(),
                    semilla.nivelDificultad(), e.getErrores());
        }
    }

    @Test
    void elTextoAMemorizarEstaEnMemoriaYSoloEnMemoria() {
        for (EjercicioSemilla s : semillas()) {
            boolean tieneTexto = s.textoAMemorizar() != null && !s.textoAMemorizar().isBlank();
            assertThat(tieneTexto).as(s.consigna()).isEqualTo(s.categoria() == CategoriaEjercicio.MEMORIA);
        }
    }

    @Test
    void losConteosPorCategoriaFormatoYNivelSonLosEsperados() {
        assertThat(contar(EjercicioSemilla::categoria)).containsExactlyInAnyOrderEntriesOf(Map.of(
                CategoriaEjercicio.REFRAN, 6L, CategoriaEjercicio.SERIE, 10L,
                CategoriaEjercicio.ASOCIACION, 8L, CategoriaEjercicio.MEMORIA, 12L));
        assertThat(contar(EjercicioSemilla::formato)).containsExactlyInAnyOrderEntriesOf(Map.of(
                FormatoEjercicio.OPCION_MULTIPLE, 33L, FormatoEjercicio.ORDENAR, 3L));
        assertThat(contar(EjercicioSemilla::nivelDificultad)).containsExactlyInAnyOrderEntriesOf(Map.of(
                NivelDificultad.BASICO, 8L, NivelDificultad.INTERMEDIO, 16L, NivelDificultad.AVANZADO, 12L));
    }

    private static <T> Map<T, Long> contar(Function<EjercicioSemilla, T> campo) {
        return semillas().stream().collect(Collectors.groupingBy(campo, Collectors.counting()));
    }
}
