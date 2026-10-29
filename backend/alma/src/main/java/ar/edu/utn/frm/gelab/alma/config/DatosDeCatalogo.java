package ar.edu.utn.frm.gelab.alma.config;

import java.time.LocalDate;

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
 * Es idempotente: un ejercicio se identifica por su consigna y una microcápsula por su título,
 * aunque esté dado de baja (así no se recrea lo que la persona operadora eliminó).
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDeCatalogo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeCatalogo.class);

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
        creados += opcionMultiple(CategoriaEjercicio.SERIE, NivelDificultad.BASICO,
                "¿Qué número sigue? 2, 4, 6, 8…",
                "Cada número es 2 más que el anterior.",
                null,
                "10", "9", "12");
        creados += ordenar(CategoriaEjercicio.SERIE, NivelDificultad.BASICO,
                "Ordená los pasos para lavarte las manos",
                "Lavarse bien las manos ayuda a prevenir enfermedades.",
                "Mojarte las manos con agua", "Ponerte jabón", "Frotar las manos durante 20 segundos",
                "Enjuagarte y secarte con una toalla limpia");
        creados += opcionMultiple(CategoriaEjercicio.ASOCIACION, NivelDificultad.BASICO,
                "¿Qué objeto usás cuando llueve?",
                "El paraguas nos protege de la lluvia.",
                null,
                "Paraguas", "Anteojos de sol", "Pelota");
        creados += opcionMultiple(CategoriaEjercicio.MEMORIA, NivelDificultad.BASICO,
                "¿Cuál de estos productos estaba en la lista de compras?",
                "Este ejercicio entrena la memoria: primero leés la lista y después la recordás.",
                "Leé con atención esta lista de compras: pan, leche, tomates y yerba.",
                "Yerba", "Café", "Arroz", "Queso");
        return creados;
    }

    private int cargarMicrocapsulas() {
        int creadas = 0;
        creadas += microcapsula("Una casa sin tropiezos", CategoriaMicrocapsula.PREVENCION_CAIDAS,
                LocalDate.of(2026, 10, 1),
                "Sacá las alfombras chicas que se mueven y dejá libres los pasillos. "
                        + "Dejá una luz encendida de noche para ir al baño sin tropezar. "
                        + "Si podés, poné agarraderas en la ducha y al lado del inodoro.");
        creadas += microcapsula("Tomá agua aunque no tengas sed", CategoriaMicrocapsula.ALIMENTACION,
                LocalDate.of(2026, 10, 2),
                "Con los años, la sensación de sed disminuye. Tené un vaso o una botella de agua a mano "
                        + "y tomá pequeños sorbos durante el día. Las frutas y las sopas también suman.");
        creadas += microcapsula("Caminar un rato todos los días", CategoriaMicrocapsula.ACTIVIDAD_FISICA,
                LocalDate.of(2026, 10, 3),
                "Una caminata corta, a tu ritmo, ayuda al corazón, a los músculos y al ánimo. "
                        + "Usá calzado cómodo y elegí lugares planos y bien iluminados. "
                        + "Si tenés alguna duda, consultá con tu médico.");
        creadas += microcapsula("Dormir mejor", CategoriaMicrocapsula.SUENIO_DESCANSO,
                LocalDate.of(2026, 10, 4),
                "Tratá de acostarte y levantarte siempre a la misma hora. "
                        + "Evitá el café y el mate a la tarde, y dejá la tele y el celular un rato antes de dormir.");
        creadas += microcapsula("Charlar le hace bien al ánimo", CategoriaMicrocapsula.BIENESTAR_EMOCIONAL,
                LocalDate.of(2026, 10, 5),
                "Llamar a un familiar, conversar con una vecina o sumarte a un grupo del barrio "
                        + "ayuda a sentirse acompañado. Contar cómo te sentís también es una forma de cuidarte.");
        return creadas;
    }

    /** La primera opción es la correcta. */
    private int opcionMultiple(CategoriaEjercicio categoria, NivelDificultad nivel, String consigna,
                               String explicacion, String textoAMemorizar, String... opciones) {
        if (ejercicioRepository.existsByConsigna(consigna)) {
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
        if (ejercicioRepository.existsByConsigna(consigna)) {
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

    private int microcapsula(String titulo, CategoriaMicrocapsula categoria, LocalDate fechaPublicacion,
                             String texto) {
        if (microcapsulaRepository.existsByTitulo(titulo)) {
            return 0;
        }
        Microcapsula microcapsula = new Microcapsula();
        microcapsula.setTitulo(titulo);
        microcapsula.setTexto(texto);
        microcapsula.setCategoria(categoria);
        microcapsula.setFechaPublicacion(fechaPublicacion);
        microcapsulaRepository.save(microcapsula);
        return 1;
    }
}
