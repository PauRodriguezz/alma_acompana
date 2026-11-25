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

import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;

/**
 * Microcápsulas de ejemplo para la pantalla Leer (contrato de microcápsulas, anexo A). Se suman a las 5
 * de DatosDeCatalogo: así cada categoría tiene al menos una y hay textos de distinto largo, con párrafos.
 * Los textos están en {@code src/main/resources/microcapsulas/} (uno por archivo).
 * Solo con alma.seed.enabled=true. Es idempotente por título, aunque la microcápsula esté dada de baja
 * (así no se recrea lo que la persona operadora eliminó). Si un título ya existe, no se toca su texto.
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDeCatalogoMicrocapsulas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeCatalogoMicrocapsulas.class);

    /** Las microcápsulas que carga este seed (también las usa la prueba de límites y formato). */
    public static final List<MicrocapsulaSemilla> SEMILLAS = List.of(
            new MicrocapsulaSemilla("Un botiquín listo en casa", CategoriaMicrocapsula.CUIDADO_HOGAR,
                    LocalDate.of(2026, 10, 6), "botiquin-listo-en-casa.txt"),
            new MicrocapsulaSemilla("Preguntá todo lo que quieras en la consulta",
                    CategoriaMicrocapsula.RECOMENDACIONES,
                    LocalDate.of(2026, 10, 7), "preguntar-en-la-consulta.txt"),
            new MicrocapsulaSemilla("Levantate despacio", CategoriaMicrocapsula.PREVENCION_CAIDAS,
                    LocalDate.of(2026, 10, 8), "levantate-despacio.txt"),
            new MicrocapsulaSemilla("Comer bien todos los días", CategoriaMicrocapsula.ALIMENTACION,
                    LocalDate.of(2026, 10, 9), "comer-bien-todos-los-dias.txt"),
            new MicrocapsulaSemilla("Ejercicios suaves para empezar el día", CategoriaMicrocapsula.ACTIVIDAD_FISICA,
                    LocalDate.of(2026, 10, 10), "ejercicios-suaves-para-empezar-el-dia.txt"));

    private final MicrocapsulaRepository microcapsulaRepository;

    public DatosDeCatalogoMicrocapsulas(MicrocapsulaRepository microcapsulaRepository) {
        this.microcapsulaRepository = microcapsulaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creadas = 0;
        for (MicrocapsulaSemilla semilla : SEMILLAS) {
            creadas += microcapsula(semilla);
        }
        if (creadas > 0) {
            log.info("Microcápsulas de lectura de ejemplo: {} creadas", creadas);
        }
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
