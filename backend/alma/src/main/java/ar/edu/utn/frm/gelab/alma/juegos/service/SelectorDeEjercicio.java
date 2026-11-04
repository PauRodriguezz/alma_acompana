package ar.edu.utn.frm.gelab.alma.juegos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;

/**
 * Qué ejercicio sigue y en qué orden se muestran sus opciones (contrato de juegos, 5.1 y 5.3).
 * Lógica pura: no toca la base. El azar se inyecta para que las pruebas sean determinísticas.
 */
@Component
public class SelectorDeEjercicio {

    /** Rotación fija de categorías (sección 2). */
    static final List<CategoriaEjercicio> ROTACION = List.of(CategoriaEjercicio.REFRAN,
            CategoriaEjercicio.SERIE, CategoriaEjercicio.ASOCIACION, CategoriaEjercicio.MEMORIA);

    private final RandomGenerator azar;

    public SelectorDeEjercicio() {
        this(new Random());
    }

    SelectorDeEjercicio(RandomGenerator azar) {
        this.azar = azar;
    }

    /**
     * Elige el próximo ejercicio.
     *
     * @param nivelActual      nivel de la persona (BASICO si no tiene progreso)
     * @param ultimoEjercicio  último respondido, o null; puede estar dado de baja
     * @param activosDelNivel  ejercicios activos de un nivel; se pide solo cuando hace falta
     * @param ultimaVezJugado  fecha y hora del último intento por id de ejercicio; los que no están nunca se jugaron
     * @return vacío si no hay ningún ejercicio activo en ningún nivel
     */
    public Optional<Ejercicio> elegir(NivelDificultad nivelActual, Ejercicio ultimoEjercicio,
            Function<NivelDificultad, List<Ejercicio>> activosDelNivel, Map<Long, LocalDateTime> ultimaVezJugado) {
        CategoriaEjercicio partida = categoriaDePartida(ultimoEjercicio);
        Long ultimoId = ultimoEjercicio != null ? ultimoEjercicio.getId() : null;

        for (NivelDificultad nivel : ordenDeNiveles(nivelActual)) {
            List<Ejercicio> pool = activosDelNivel.apply(nivel);
            if (pool == null || pool.isEmpty()) {
                continue;
            }
            List<Ejercicio> elegibles = pool.stream().filter(e -> !e.getId().equals(ultimoId)).toList();
            if (elegibles.isEmpty()) {
                elegibles = pool; // C.A. Nº1: el último es el único del nivel.
            }
            int inicio = ROTACION.indexOf(partida);
            for (int i = 0; i < ROTACION.size(); i++) {
                CategoriaEjercicio categoria = ROTACION.get((inicio + i) % ROTACION.size());
                List<Ejercicio> deLaCategoria = elegibles.stream().filter(e -> e.getCategoria() == categoria).toList();
                if (!deLaCategoria.isEmpty()) {
                    return Optional.of(jugadoHaceMasTiempo(deLaCategoria, ultimaVezJugado));
                }
            }
        }
        return Optional.empty();
    }

    /** La que sigue a la del último ejercicio en la rotación; sin último, REFRAN. */
    static CategoriaEjercicio categoriaDePartida(Ejercicio ultimoEjercicio) {
        if (ultimoEjercicio == null) {
            return ROTACION.getFirst();
        }
        int indice = ROTACION.indexOf(ultimoEjercicio.getCategoria());
        return ROTACION.get((indice + 1) % ROTACION.size());
    }

    /** Sección 5.3: primero el nivel actual y después el más cercano. */
    static List<NivelDificultad> ordenDeNiveles(NivelDificultad nivelActual) {
        return switch (nivelActual) {
            case BASICO -> List.of(NivelDificultad.BASICO, NivelDificultad.INTERMEDIO, NivelDificultad.AVANZADO);
            case INTERMEDIO -> List.of(NivelDificultad.INTERMEDIO, NivelDificultad.BASICO, NivelDificultad.AVANZADO);
            case AVANZADO -> List.of(NivelDificultad.AVANZADO, NivelDificultad.INTERMEDIO, NivelDificultad.BASICO);
        };
    }

    /** Los nunca jugados primero; si no, el de intento más viejo. Empate: al azar. */
    private Ejercicio jugadoHaceMasTiempo(List<Ejercicio> candidatos, Map<Long, LocalDateTime> ultimaVezJugado) {
        Function<Ejercicio, LocalDateTime> ultimaVez =
                e -> ultimaVezJugado.getOrDefault(e.getId(), LocalDateTime.MIN);
        LocalDateTime masVieja = candidatos.stream().map(ultimaVez).min(Comparator.naturalOrder()).orElseThrow();
        // Orden por id para que, con el mismo azar, el resultado sea siempre el mismo.
        List<Ejercicio> empatados = candidatos.stream()
                .filter(e -> ultimaVez.apply(e).equals(masVieja))
                .sorted(Comparator.comparing(Ejercicio::getId))
                .toList();
        return empatados.get(azar.nextInt(empatados.size()));
    }

    /**
     * Opciones en orden al azar (sección 2, "Mezcla"). En ORDENAR nunca quedan en el orden correcto:
     * si la mezcla coincide, se corren un lugar.
     */
    public List<OpcionRespuesta> mezclar(List<OpcionRespuesta> opciones, FormatoEjercicio formato) {
        List<OpcionRespuesta> mezcladas = new ArrayList<>(opciones);
        Collections.shuffle(mezcladas, azar);
        if (formato == FormatoEjercicio.ORDENAR && mezcladas.size() > 1 && mezcladas.equals(ordenCorrecto(opciones))) {
            Collections.rotate(mezcladas, 1);
        }
        return mezcladas;
    }

    /** Pasos de un ejercicio de ordenar en el orden correcto. */
    public static List<OpcionRespuesta> ordenCorrecto(List<OpcionRespuesta> opciones) {
        return opciones.stream()
                .sorted(Comparator.comparing(OpcionRespuesta::getOrdenCorrecto,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
