package ar.edu.utn.frm.gelab.alma.juegos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;

/**
 * Selección del próximo ejercicio y mezcla de opciones (contrato de juegos, 5.1 y 5.3). Sin base de datos.
 */
class SelectorDeEjercicioTest {

    private static final LocalDateTime AYER = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final LocalDateTime HACE_UNA_SEMANA = LocalDateTime.of(2026, 10, 28, 10, 0);

    /** Azar fijo: nextInt(bound) devuelve siempre el mismo índice (acotado al rango). */
    private static RandomGenerator azarFijo(int indice) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(indice, bound - 1);
            }
        };
    }

    /** Azar que nunca mueve nada en Collections.shuffle: devuelve siempre bound - 1. */
    private static final RandomGenerator AZAR_IDENTIDAD = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    };

    private final SelectorDeEjercicio selector = new SelectorDeEjercicio(azarFijo(0));

    private final Map<NivelDificultad, List<Ejercicio>> catalogo = new EnumMap<>(NivelDificultad.class);
    private final Map<Long, LocalDateTime> ultimaVez = new HashMap<>();

    private Ejercicio ejercicio(long id, CategoriaEjercicio categoria, NivelDificultad nivel) {
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setId(id);
        ejercicio.setCategoria(categoria);
        ejercicio.setNivelDificultad(nivel);
        ejercicio.setFormato(FormatoEjercicio.OPCION_MULTIPLE);
        catalogo.computeIfAbsent(nivel, n -> new ArrayList<>()).add(ejercicio);
        return ejercicio;
    }

    private Optional<Ejercicio> elegir(SelectorDeEjercicio selector, NivelDificultad nivel, Ejercicio ultimo) {
        return selector.elegir(nivel, ultimo, n -> catalogo.getOrDefault(n, List.of()), ultimaVez);
    }

    private Optional<Ejercicio> elegir(NivelDificultad nivel, Ejercicio ultimo) {
        return elegir(selector, nivel, ultimo);
    }

    // Rotación de categorías

    @Test
    void sinUltimoEmpiezaPorRefran() {
        ejercicio(1, CategoriaEjercicio.SERIE, NivelDificultad.BASICO);
        Ejercicio refran = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        ejercicio(3, CategoriaEjercicio.MEMORIA, NivelDificultad.BASICO);

        assertThat(elegir(NivelDificultad.BASICO, null)).contains(refran);
    }

    @Test
    void rotaRefranSerieAsociacionMemoriaYVuelveARefran() {
        Ejercicio refran = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio serie = ejercicio(2, CategoriaEjercicio.SERIE, NivelDificultad.BASICO);
        Ejercicio asociacion = ejercicio(3, CategoriaEjercicio.ASOCIACION, NivelDificultad.BASICO);
        Ejercicio memoria = ejercicio(4, CategoriaEjercicio.MEMORIA, NivelDificultad.BASICO);

        assertThat(elegir(NivelDificultad.BASICO, refran)).contains(serie);
        assertThat(elegir(NivelDificultad.BASICO, serie)).contains(asociacion);
        assertThat(elegir(NivelDificultad.BASICO, asociacion)).contains(memoria);
        assertThat(elegir(NivelDificultad.BASICO, memoria)).contains(refran);
    }

    @Test
    void categoriaSinEjerciciosPasaALaSiguiente() {
        Ejercicio refran = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio asociacion = ejercicio(2, CategoriaEjercicio.ASOCIACION, NivelDificultad.BASICO);
        // No hay SERIE en BASICO.

        assertThat(elegir(NivelDificultad.BASICO, refran)).contains(asociacion);
    }

    @Test
    void ultimoDadoDeBajaIgualMarcaLaRotacion() {
        Ejercicio dadoDeBaja = new Ejercicio();
        dadoDeBaja.setId(99L);
        dadoDeBaja.setCategoria(CategoriaEjercicio.SERIE);
        ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio asociacion = ejercicio(2, CategoriaEjercicio.ASOCIACION, NivelDificultad.BASICO);

        assertThat(elegir(NivelDificultad.BASICO, dadoDeBaja)).contains(asociacion);
    }

    // Exclusión del último y C.A. Nº1

    @Test
    void noRepiteElUltimoSiHayOtroEnElNivel() {
        Ejercicio ultimo = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio otro = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        // Solo hay refranes: da la vuelta entera y vuelve a REFRAN, sin el último.

        assertThat(elegir(NivelDificultad.BASICO, ultimo)).contains(otro);
    }

    @Test
    void noRepiteElUltimoAunqueSeaElJugadoHaceMasTiempo() {
        Ejercicio ultimo = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio otro = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        ultimaVez.put(ultimo.getId(), HACE_UNA_SEMANA);
        ultimaVez.put(otro.getId(), AYER);

        assertThat(elegir(NivelDificultad.BASICO, ultimo)).contains(otro);
    }

    @Test
    void siElUltimoEsElUnicoDelNivelSeRepite() {
        Ejercicio unico = ejercicio(1, CategoriaEjercicio.MEMORIA, NivelDificultad.BASICO);
        ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.INTERMEDIO);

        assertThat(elegir(NivelDificultad.BASICO, unico)).contains(unico);
    }

    // Nivel más cercano

    @Test
    void ordenDeNivelesSegunSeccion53() {
        assertThat(SelectorDeEjercicio.ordenDeNiveles(NivelDificultad.BASICO)).containsExactly(
                NivelDificultad.BASICO, NivelDificultad.INTERMEDIO, NivelDificultad.AVANZADO);
        assertThat(SelectorDeEjercicio.ordenDeNiveles(NivelDificultad.INTERMEDIO)).containsExactly(
                NivelDificultad.INTERMEDIO, NivelDificultad.BASICO, NivelDificultad.AVANZADO);
        assertThat(SelectorDeEjercicio.ordenDeNiveles(NivelDificultad.AVANZADO)).containsExactly(
                NivelDificultad.AVANZADO, NivelDificultad.INTERMEDIO, NivelDificultad.BASICO);
    }

    @Test
    void usaElNivelActualAntesQueOtro() {
        ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio intermedio = ejercicio(2, CategoriaEjercicio.MEMORIA, NivelDificultad.INTERMEDIO);

        assertThat(elegir(NivelDificultad.INTERMEDIO, null)).contains(intermedio);
    }

    @Test
    void nivelVacioPasaAlMasCercano() {
        Ejercicio basico = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.AVANZADO);

        // INTERMEDIO vacío: prueba BASICO antes que AVANZADO.
        assertThat(elegir(NivelDificultad.INTERMEDIO, null)).contains(basico);
    }

    @Test
    void desdeAvanzadoPruebaIntermedioAntesQueBasico() {
        ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio intermedio = ejercicio(2, CategoriaEjercicio.SERIE, NivelDificultad.INTERMEDIO);

        assertThat(elegir(NivelDificultad.AVANZADO, null)).contains(intermedio);
    }

    @Test
    void desdeBasicoVacioLlegaHastaAvanzado() {
        Ejercicio avanzado = ejercicio(1, CategoriaEjercicio.ASOCIACION, NivelDificultad.AVANZADO);

        assertThat(elegir(NivelDificultad.BASICO, null)).contains(avanzado);
    }

    // Jugado hace más tiempo y empate al azar

    @Test
    void eligeElJugadoHaceMasTiempo() {
        Ejercicio reciente = ejercicio(1, CategoriaEjercicio.SERIE, NivelDificultad.BASICO);
        Ejercicio viejo = ejercicio(2, CategoriaEjercicio.SERIE, NivelDificultad.BASICO);
        ultimaVez.put(reciente.getId(), AYER);
        ultimaVez.put(viejo.getId(), HACE_UNA_SEMANA);

        assertThat(elegir(NivelDificultad.BASICO, null)).contains(viejo);
    }

    @Test
    void losNuncaJugadosVanPrimero() {
        Ejercicio viejo = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio nuevo = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        ultimaVez.put(viejo.getId(), HACE_UNA_SEMANA);

        assertThat(elegir(NivelDificultad.BASICO, null)).contains(nuevo);
    }

    @Test
    void empateSeResuelveConElAzar() {
        Ejercicio primero = ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio segundo = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio tercero = ejercicio(3, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);

        assertThat(elegir(new SelectorDeEjercicio(azarFijo(0)), NivelDificultad.BASICO, null)).contains(primero);
        assertThat(elegir(new SelectorDeEjercicio(azarFijo(1)), NivelDificultad.BASICO, null)).contains(segundo);
        assertThat(elegir(new SelectorDeEjercicio(azarFijo(2)), NivelDificultad.BASICO, null)).contains(tercero);
    }

    @Test
    void elAzarSoloDesempataEntreLosMasViejos() {
        ejercicio(1, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio viejoA = ejercicio(2, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        Ejercicio viejoB = ejercicio(3, CategoriaEjercicio.REFRAN, NivelDificultad.BASICO);
        ultimaVez.put(1L, AYER);
        ultimaVez.put(2L, HACE_UNA_SEMANA);
        ultimaVez.put(3L, HACE_UNA_SEMANA);

        assertThat(elegir(new SelectorDeEjercicio(azarFijo(0)), NivelDificultad.BASICO, null)).contains(viejoA);
        assertThat(elegir(new SelectorDeEjercicio(azarFijo(5)), NivelDificultad.BASICO, null)).contains(viejoB);
    }

    // Sin ejercicios

    @Test
    void sinEjerciciosActivosDevuelveVacio() {
        assertThat(elegir(NivelDificultad.BASICO, null)).isEmpty();
        assertThat(elegir(NivelDificultad.AVANZADO, null)).isEmpty();
    }

    // Mezcla de opciones

    private static OpcionRespuesta opcion(long id, Integer ordenCorrecto) {
        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setId(id);
        opcion.setTexto("Opción " + id);
        opcion.setEsCorrecta(false);
        opcion.setOrdenCorrecto(ordenCorrecto);
        return opcion;
    }

    @Test
    void mezclarConservaTodasLasOpciones() {
        List<OpcionRespuesta> opciones = List.of(opcion(1, null), opcion(2, null), opcion(3, null), opcion(4, null));

        assertThat(new SelectorDeEjercicio(new Random(7)).mezclar(opciones, FormatoEjercicio.OPCION_MULTIPLE))
                .containsExactlyInAnyOrderElementsOf(opciones);
    }

    @Test
    void ordenarNuncaDevuelveElOrdenCorrecto() {
        // Las opciones llegan en el orden correcto y el azar no las mueve: igual tienen que cambiar.
        List<OpcionRespuesta> pasos = List.of(opcion(1, 1), opcion(2, 2), opcion(3, 3), opcion(4, 4));

        List<OpcionRespuesta> mezcladas = new SelectorDeEjercicio(AZAR_IDENTIDAD).mezclar(pasos, FormatoEjercicio.ORDENAR);

        assertThat(mezcladas).containsExactlyInAnyOrderElementsOf(pasos);
        assertThat(mezcladas).isNotEqualTo(pasos);
    }

    @Test
    void ordenarNuncaDevuelveElOrdenCorrectoConMuchasMezclas() {
        // Opciones guardadas desordenadas: lo que se compara es el ordenCorrecto, no el orden de la lista.
        List<OpcionRespuesta> pasos = List.of(opcion(3, 3), opcion(1, 1), opcion(2, 2));
        List<OpcionRespuesta> correcto = SelectorDeEjercicio.ordenCorrecto(pasos);
        SelectorDeEjercicio conAzar = new SelectorDeEjercicio(new Random(42));

        for (int i = 0; i < 500; i++) {
            assertThat(conAzar.mezclar(pasos, FormatoEjercicio.ORDENAR)).isNotEqualTo(correcto);
        }
    }

    @Test
    void ordenCorrectoOrdenaPorOrdenCorrecto() {
        OpcionRespuesta uno = opcion(10, 1);
        OpcionRespuesta dos = opcion(11, 2);
        OpcionRespuesta tres = opcion(12, 3);

        assertThat(SelectorDeEjercicio.ordenCorrecto(List.of(tres, uno, dos))).containsExactly(uno, dos, tres);
    }
}
