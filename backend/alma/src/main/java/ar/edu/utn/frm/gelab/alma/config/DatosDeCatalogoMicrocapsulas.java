package ar.edu.utn.frm.gelab.alma.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;

/**
 * Microcápsulas de ejemplo para la pantalla Leer (contrato de microcápsulas, anexo A). Se suman a las 5
 * de DatosDeCatalogo: así cada categoría tiene al menos una y hay textos de distinto largo, con párrafos.
 * Solo con alma.seed.enabled=true. Es idempotente por título, aunque la microcápsula esté dada de baja
 * (así no se recrea lo que la persona operadora eliminó).
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDeCatalogoMicrocapsulas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeCatalogoMicrocapsulas.class);

    private final MicrocapsulaRepository microcapsulaRepository;

    public DatosDeCatalogoMicrocapsulas(MicrocapsulaRepository microcapsulaRepository) {
        this.microcapsulaRepository = microcapsulaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creadas = 0;
        creadas += microcapsula("Un botiquín listo en casa", CategoriaMicrocapsula.CUIDADO_HOGAR,
                LocalDate.of(2026, 10, 6),
                "Tené en un lugar fijo y a mano un termómetro, gasas, curitas y alcohol en gel. "
                        + "Sumá una lista con los teléfonos de tu médico y de un familiar.\n\n"
                        + "Revisá de vez en cuando las fechas de vencimiento de lo que guardás.\n\n"
                        + "Guardá los medicamentos en su caja original, lejos del calor y de la humedad.");
        creadas += microcapsula("Preguntá todo lo que quieras en la consulta", CategoriaMicrocapsula.RECOMENDACIONES,
                LocalDate.of(2026, 10, 7),
                "Antes de ir al médico, anotá tus dudas y los medicamentos que tomás. "
                        + "Llevá esa lista y mostrásela.\n\n"
                        + "Si no entendés algo, pedí que te lo expliquen de nuevo con palabras simples. "
                        + "Es tu derecho y es parte de cuidarte.\n\n"
                        + "Podés ir acompañado por alguien de confianza. Al terminar, pedí que te dejen "
                        + "las indicaciones por escrito para no olvidarte de nada.");
        creadas += microcapsula("Levantate despacio", CategoriaMicrocapsula.PREVENCION_CAIDAS,
                LocalDate.of(2026, 10, 8),
                "Al salir de la cama o de una silla, quedate un momento sentado en el borde y respirá tranquilo.\n\n"
                        + "Después apoyá bien los pies en el piso y ponete de pie despacio, "
                        + "con algo firme para agarrarte.\n\n"
                        + "Así se evitan los mareos y los tropiezos. Si te mareás seguido, contáselo a tu médico.");
        creadas += microcapsula("Comer bien todos los días", CategoriaMicrocapsula.ALIMENTACION,
                LocalDate.of(2026, 10, 9),
                "Tratá de sumar verduras y frutas de distintos colores en tus comidas.\n\n"
                        + "Repartí lo que comés en varias veces al día, sin saltearte ninguna.\n\n"
                        + "Para darle sabor a la comida, probá con limón, ajo o hierbas en lugar de sal.\n\n"
                        + "Si tenés una dieta indicada por tu médico, seguí lo que te recomendó.");
        creadas += microcapsula("Ejercicios suaves para empezar el día", CategoriaMicrocapsula.ACTIVIDAD_FISICA,
                LocalDate.of(2026, 10, 10),
                "Antes de empezar el día, mové con suavidad el cuello, los hombros, los brazos y las piernas. "
                        + "Podés hacerlo sentado o parado con un apoyo.\n\n"
                        + "No hace falta forzar nada ni sentir dolor: unos pocos minutos alcanzan.\n\n"
                        + "Si algún movimiento te duele, dejalo y consultá con tu médico.");
        if (creadas > 0) {
            log.info("Microcápsulas de lectura de ejemplo: {} creadas", creadas);
        }
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
