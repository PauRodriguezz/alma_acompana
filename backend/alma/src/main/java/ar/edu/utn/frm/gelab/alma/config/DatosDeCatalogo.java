package ar.edu.utn.frm.gelab.alma.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;

/**
 * Ejercicios y microcápsulas de ejemplo para el panel de operador. Solo con alma.seed.enabled=true.
 * Es idempotente: un ejercicio se identifica por su categoría, nivel y consigna, y una microcápsula por su
 * título, aunque esté dado de baja (así no se recrea lo que la persona operadora eliminó). Si un título ya existe,
 * no se toca su texto. Los textos de las microcápsulas están en {@code src/main/resources/microcapsulas/}.
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDeCatalogo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeCatalogo.class);

    /** Las microcápsulas que carga este seed (también las usa la prueba de límites y formato). */
    public static final List<MicrocapsulaSemilla> SEMILLAS = List.of(
            new MicrocapsulaSemilla("Una casa sin tropiezos", CategoriaMicrocapsula.PREVENCION_CAIDAS,
                    LocalDate.of(2026, 10, 1), "casa-sin-tropiezos.txt"),
            new MicrocapsulaSemilla("Tomá agua aunque no tengas sed", CategoriaMicrocapsula.ALIMENTACION,
                    LocalDate.of(2026, 10, 2), "tomar-agua-aunque-no-tengas-sed.txt"),
            new MicrocapsulaSemilla("Caminar un rato todos los días", CategoriaMicrocapsula.ACTIVIDAD_FISICA,
                    LocalDate.of(2026, 10, 3), "caminar-todos-los-dias.txt"),
            new MicrocapsulaSemilla("Dormir mejor", CategoriaMicrocapsula.SUENIO_DESCANSO,
                    LocalDate.of(2026, 10, 4), "dormir-mejor.txt"),
            new MicrocapsulaSemilla("Charlar le hace bien al ánimo", CategoriaMicrocapsula.BIENESTAR_EMOCIONAL,
                    LocalDate.of(2026, 10, 5), "charlar-le-hace-bien-al-animo.txt"));

    private final EjercicioRepository ejercicioRepository;
    private final MicrocapsulaRepository microcapsulaRepository;

    public DatosDeCatalogo(EjercicioRepository ejercicioRepository,
                           MicrocapsulaRepository microcapsulaRepository) {
        this.ejercicioRepository = ejercicioRepository;
        this.microcapsulaRepository = microcapsulaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int ejercicios = cargarEjercicios();
        int microcapsulas = cargarMicrocapsulas();
        if (ejercicios + microcapsulas > 0) {
            log.info("Catálogo de ejemplo: {} ejercicios y {} microcápsulas creados", ejercicios, microcapsulas);
        }
    }

    private int cargarEjercicios() {
        int creados = 0;
        creados += opcionMultiple(CategoriaEjercicio.REFRAN, NivelDificultad.BASICO,
                "Completá el refrán: «Más vale pájaro en mano…»",
                "Este ejercicio trata sobre completar con la palabra correcta el refrán que te aparece.",
                null,
                "…que cien volando", "…que dos en el aire", "…que uno en la mano");
        creados += opcionMultiple(CategoriaEjercicio.REFRAN, NivelDificultad.INTERMEDIO,
                "Completá el refrán: «No por mucho madrugar…»",
                "Quiere decir que apurarse no siempre hace que las cosas salgan antes.",
                null,
                "…amanece más temprano", "…se llega más lejos", "…se duerme menos", "…se trabaja mejor");
        creados += opcionMultiple(CategoriaEjercicio.MEMORIA, NivelDificultad.BASICO,
                "¿Cuál de estos productos estaba en la lista de compras?",
                "Este ejercicio entrena la memoria: primero leés la lista y después la recordás.",
                "Leé con atención esta lista de compras: pan, leche, tomates y yerba.",
                "Yerba", "Café", "Arroz", "Queso");
        return creados;
    }

    private int cargarMicrocapsulas() {
        int creadas = 0;
        for (MicrocapsulaSemilla semilla : SEMILLAS) {
            creadas += microcapsula(semilla);
        }
        return creadas;
    }

    /** La primera opción es la correcta. */
    private int opcionMultiple(CategoriaEjercicio categoria, NivelDificultad nivel, String consigna,
                               String explicacion, String textoAMemorizar, String... opciones) {
        if (ejercicioRepository.existsByCategoriaAndNivelDificultadAndConsigna(categoria, nivel, consigna)) {
            return 0;
        }
        Ejercicio ejercicio = ejercicio(categoria, FormatoEjercicio.OPCION_MULTIPLE, nivel, consigna,
                explicacion, textoAMemorizar);
        for (int i = 0; i < opciones.length; i++) {
            ejercicio.getOpciones().add(opcion(opciones[i], i == 0, null));
        }
        ejercicioRepository.save(ejercicio);
        return 1;
    }

    /** Los pasos van en el orden correcto. */
    private int ordenar(CategoriaEjercicio categoria, NivelDificultad nivel, String consigna,
                        String explicacion, String... pasos) {
        if (ejercicioRepository.existsByCategoriaAndNivelDificultadAndConsigna(categoria, nivel, consigna)) {
            return 0;
        }
        Ejercicio ejercicio = ejercicio(categoria, FormatoEjercicio.ORDENAR, nivel, consigna, explicacion, null);
        for (int i = 0; i < pasos.length; i++) {
            ejercicio.getOpciones().add(opcion(pasos[i], false, i + 1));
        }
        ejercicioRepository.save(ejercicio);
        return 1;
    }

    private static Ejercicio ejercicio(CategoriaEjercicio categoria, FormatoEjercicio formato, NivelDificultad nivel,
                                       String consigna, String explicacion, String textoAMemorizar) {
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setCategoria(categoria);
        ejercicio.setFormato(formato);
        ejercicio.setNivelDificultad(nivel);
        ejercicio.setConsigna(consigna);
        ejercicio.setExplicacion(explicacion);
        ejercicio.setTextoAMemorizar(textoAMemorizar);
        ejercicio.setFechaAlta(Fechas.hoy());
        return ejercicio;
    }

    private static OpcionRespuesta opcion(String texto, boolean esCorrecta, Integer ordenCorrecto) {
        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setTexto(texto);
        opcion.setEsCorrecta(esCorrecta);
        opcion.setOrdenCorrecto(ordenCorrecto);
        return opcion;
    }

    private int microcapsula(MicrocapsulaSemilla semilla) {
        if (microcapsulaRepository.existsByTitulo(semilla.titulo())) {
            return 0;
        }
        Microcapsula microcapsula = new Microcapsula();
        microcapsula.setTitulo(semilla.titulo());
        microcapsula.setTexto(TextosDeMicrocapsulas.leer(semilla.archivo()));
        microcapsula.setCategoria(semilla.categoria());
        microcapsula.setFechaPublicacion(semilla.fechaPublicacion());
        microcapsulaRepository.save(microcapsula);
        return 1;
    }
}
