package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.annotation.DirtiesContext;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.ParejasConcurrentes.Accion;
import ar.edu.utn.frm.gelab.alma.historia.ParejasConcurrentes.Resumen;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * CONTROL de {@link HistoriaConcurrenciaTest}: la misma prueba, con el mismo código de producción, pero sin
 * el bloqueo del nodo de la persona ({@code bloquearPersona} anulado en una subclase de prueba). Si acá la
 * carrera no apareciera nunca, la prueba con bloqueo no demostraría nada. Sin bloqueo, la regla de integridad
 * 3 se rompe: dos pedidos simultáneos pueden quedar los dos registrados.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@Import(ModeloDePruebaConfig.class)
// Contexto propio (escritura sin bloqueo): se cierra al terminar para liberar su pool de conexiones.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HistoriaSinBloqueoTest {

    /** {@link EscrituraDelGrafo} sin el bloqueo: todo lo demás es idéntico. */
    static class EscrituraSinBloqueo extends EscrituraDelGrafo {

        EscrituraSinBloqueo(Neo4jClient neo4jClient, Clock clock) {
            super(neo4jClient, clock);
        }

        @Override
        protected void bloquearPersona(Long personaId) {
            // Sin bloqueo.
        }
    }

    @TestConfiguration
    static class ConfigSinBloqueo {
        @Bean
        @Primary
        EscrituraSinBloqueo escrituraSinBloqueo(Neo4jClient neo4jClient, Clock clock) {
            return new EscrituraSinBloqueo(neo4jClient, clock);
        }
    }

    private static final String RESPONDER_Y_RESPONDER = "test_historia_ctrl_rr";
    private static final String RESPONDER_Y_SALTEAR = "test_historia_ctrl_rs";
    private static final String SALTEAR_Y_SALTEAR = "test_historia_ctrl_ss";
    private static final List<String> USUARIOS = List.of(RESPONDER_Y_RESPONDER, RESPONDER_Y_SALTEAR, SALTEAR_Y_SALTEAR);

    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private EscrituraDelGrafo escritura;
    @Autowired
    private Neo4jClient neo4j;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ColaDeExtracciones cola;

    private PersonasDeHistoria personas;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        USUARIOS.forEach(personas::borrar);
    }

    @AfterEach
    void limpiar() {
        esperarCola();
        USUARIOS.forEach(personas::borrar);
    }

    /** Responder encola extracciones: hay que esperar a que terminen antes de limpiar el grafo. */
    private void esperarCola() {
        assertThat(cola.esperarQueSeVacie(java.time.Duration.ofSeconds(30))).as("la cola se vació").isTrue();
    }

    @Test
    void elControlUsaLaEscrituraSinBloqueo() {
        // Sanidad: la prueba corre contra la subclase, no contra la escritura con bloqueo.
        assertThat(org.springframework.aop.support.AopUtils.getTargetClass(escritura)).isEqualTo(EscrituraSinBloqueo.class);
    }

    @Test
    void sinElBloqueoLaCarreraApareceEnAlgunaDeLasTresCombinaciones() throws Exception {
        Resumen rr = ParejasConcurrentes.correr("responder + responder", historia, neo4j,
                personas.crear(RESPONDER_Y_RESPONDER), HistoriaConcurrenciaTest.preguntas(), Accion.RESPONDER,
                Accion.RESPONDER);
        Resumen rs = ParejasConcurrentes.correr("responder + saltear", historia, neo4j,
                personas.crear(RESPONDER_Y_SALTEAR), HistoriaConcurrenciaTest.preguntas(), Accion.RESPONDER,
                Accion.SALTEAR);
        Resumen ss = ParejasConcurrentes.correr("saltear + saltear", historia, neo4j,
                personas.crear(SALTEAR_Y_SALTEAR), HistoriaConcurrenciaTest.preguntas(), Accion.SALTEAR,
                Accion.SALTEAR);
        for (Resumen resumen : List.of(rr, rs, ss)) {
            System.out.println("[CONCURRENCIA] SIN bloqueo (control), " + resumen);
        }

        int carreras = rr.carreras() + rs.carreras() + ss.carreras();
        System.out.println("[CONCURRENCIA] SIN bloqueo (control): carreras en total = " + carreras + " de "
                + (rr.pares() + rs.pares() + ss.pares()) + " pares");
        assertThat(carreras)
                .as("Sin el bloqueo tendría que aparecer la carrera; si no aparece, la prueba con bloqueo no demuestra nada")
                .isGreaterThanOrEqualTo(1);
    }
}
