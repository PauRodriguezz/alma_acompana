package ar.edu.utn.frm.gelab.alma.historia.config;

import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;

/**
 * Calienta la conexión a Neo4j al arrancar, para que el primer mensaje del chat no pierda los 2 s de la lectura de
 * recuerdos (contrato de historia de vida, 5.5.2 y límite 12). Corre en un hilo aparte, después de que la aplicación
 * está lista: no demora el arranque ni lo rompe si Neo4j no responde.
 *
 * <p>Solo lee: abre el pool con {@code verifyConnectivity} y ejecuta las mismas dos lecturas que usa el chat
 * ({@code estadoDeTodas} y {@code personasConocidas}, que son {@code MATCH}/{@code RETURN} sin escrituras) con un
 * id de persona que no existe, así que no crea ni lee datos de nadie. Se apaga con
 * {@code alma.historia.calentar-neo4j=false}. Límite: el pool cierra las conexiones inactivas con el tiempo, así que
 * esto ayuda al primer mensaje después de arrancar, no a todos los que siguen a una pausa larga.
 */
@Component
@ConditionalOnProperty(name = "alma.historia.calentar-neo4j", havingValue = "true", matchIfMissing = true)
public class CalentamientoDeNeo4j {

    private static final Logger log = LoggerFactory.getLogger(CalentamientoDeNeo4j.class);

    /** Un id que no existe: el id de las personas mayores sale de PostgreSQL y es siempre positivo. */
    static final long PERSONA_INEXISTENTE = -1L;

    private final Driver driver;
    private final LecturaDelGrafo lectura;

    public CalentamientoDeNeo4j(Driver driver, LecturaDelGrafo lectura) {
        this.driver = driver;
        this.lectura = lectura;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alIniciar() {
        Thread hilo = new Thread(this::calentar, "alma-calentamiento-neo4j");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Nunca lanza errores: ante una falla registra solo el tipo (nunca el mensaje) y sigue. */
    public void calentar() {
        long inicio = System.nanoTime();
        try {
            driver.verifyConnectivity();
            lectura.estadoDeTodas(PERSONA_INEXISTENTE);
            lectura.personasConocidas(PERSONA_INEXISTENTE);
            log.info("Neo4j calentado en {} ms", (System.nanoTime() - inicio) / 1_000_000);
        } catch (RuntimeException e) {
            log.warn("No se pudo calentar la conexión a Neo4j tras {} ms ({}); el primer mensaje del chat puede "
                    + "salir sin recuerdos", (System.nanoTime() - inicio) / 1_000_000, e.getClass().getSimpleName());
        }
    }
}
