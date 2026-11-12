package ar.edu.utn.frm.gelab.alma.historia.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carga inicial del grafo (contrato del grafo, secciones 2 y 10): las restricciones y el guion se cargan
 * siempre; la historia de ejemplo de {@code ana}, solo con {@code alma.seed.enabled=true}.
 *
 * <p>Corre con {@link ApplicationReadyEvent}, después de los {@code ApplicationRunner} de datos de prueba, así
 * que encuentra a {@code ana} ya creada en PostgreSQL. Es idempotente. Si Neo4j no está, registra un ERROR y
 * deja arrancar el resto de la aplicación: las restricciones y el guion se cargan en el próximo arranque
 * (contrato, sección 12). El log lleva solo el tipo de error.
 */
@Component
public class CargaInicialDelGrafo implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialDelGrafo.class);

    private static final String RECURSO_GUION = "historia/guion.json";
    private static final String RECURSO_RESTRICCIONES = "historia/restricciones.cypher";
    private static final String USUARIO_DE_EJEMPLO = "ana";

    private final EscrituraDelGrafo escritura;
    private final JsonMapper jsonMapper;
    private final CuentaRepository cuentaRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final boolean cargarEjemplos;

    public CargaInicialDelGrafo(EscrituraDelGrafo escritura, JsonMapper jsonMapper,
            CuentaRepository cuentaRepository, PersonaMayorRepository personaMayorRepository,
            @Value("${alma.seed.enabled:false}") boolean cargarEjemplos) {
        this.escritura = escritura;
        this.jsonMapper = jsonMapper;
        this.cuentaRepository = cuentaRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.cargarEjemplos = cargarEjemplos;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent evento) {
        cargar();
    }

    /** Restricciones, guion y (con el seed activo) la historia de {@code ana}. No lanza errores hacia afuera. */
    public void cargar() {
        try {
            List<String> restricciones = leerRestricciones();
            escritura.crearRestricciones(restricciones);
            int nuevas = cargarGuion(leerGuion());
            log.info("Grafo: {} restricciones aseguradas y {} preguntas nuevas del guion", restricciones.size(),
                    nuevas);
            if (cargarEjemplos) {
                cargarHistoriaDeAna();
            }
        } catch (RuntimeException e) {
            log.error("No se pudo cargar el grafo inicial ({}). Las restricciones y el guion se cargan en el "
                    + "próximo arranque.", e.getClass().getSimpleName());
        }
    }

    /** Crea las preguntas que faltan y no modifica las que ya están. @return cuántas creó */
    public int cargarGuion(List<PreguntaDelGuion> preguntas) {
        return escritura.crearPreguntasFaltantes(preguntas);
    }

    /** Carga la historia de ejemplo (Anexo B) si la persona no tiene recuerdos ni progreso. */
    public boolean cargarHistoriaDeEjemplo(Long personaId) {
        return escritura.cargarHistoriaDeEjemplo(personaId);
    }

    public List<PreguntaDelGuion> leerGuion() {
        try (InputStream entrada = new ClassPathResource(RECURSO_GUION).getInputStream()) {
            return jsonMapper.readValue(entrada, new TypeReference<List<PreguntaDelGuion>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + RECURSO_GUION, e);
        }
    }

    /** Las sentencias de {@code historia/restricciones.cypher}, sin comentarios ni vacías. */
    public List<String> leerRestricciones() {
        try (InputStream entrada = new ClassPathResource(RECURSO_RESTRICCIONES).getInputStream()) {
            String texto = StreamUtils.copyToString(entrada, StandardCharsets.UTF_8);
            String sinComentarios = String.join("\n", texto.lines().filter(l -> !l.trim().startsWith("//")).toList());
            return Arrays.stream(sinComentarios.split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + RECURSO_RESTRICCIONES, e);
        }
    }

    private void cargarHistoriaDeAna() {
        Optional<PersonaMayor> ana = cuentaRepository.findByNombreUsuario(USUARIO_DE_EJEMPLO)
                .flatMap(cuenta -> personaMayorRepository.findByCuentaId(cuenta.getId()));
        if (ana.isEmpty()) {
            log.info("Historia de ejemplo: no existe el usuario de prueba '{}'", USUARIO_DE_EJEMPLO);
            return;
        }
        if (!cargarHistoriaDeEjemplo(ana.get().getId())) {
            log.info("Historia de ejemplo: '{}' ya tiene recuerdos o progreso, no se le agrega nada",
                    USUARIO_DE_EJEMPLO);
        }
    }
}
