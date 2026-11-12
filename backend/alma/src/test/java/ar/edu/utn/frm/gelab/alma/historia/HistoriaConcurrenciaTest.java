package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.ParejasConcurrentes.Accion;
import ar.edu.utn.frm.gelab.alma.historia.ParejasConcurrentes.Resumen;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * El bloqueo del nodo {@code PersonaMayor} (contrato del grafo, sección 2, "Concurrencia en responder y
 * saltear"): dos pedidos de la misma persona sobre la misma pregunta, a la vez, nunca dejan los dos
 * registrados. Las tres combinaciones, con 20 preguntas cada una y una persona distinta por combinación. La
 * primera pareja de cada combinación además compite por crear el nodo de una persona nueva. El control
 * sin bloqueo está en {@link HistoriaSinBloqueoTest}.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@Import(ModeloDePruebaConfig.class)
class HistoriaConcurrenciaTest {

    static final int PARES = 20;

    private static final String RESPONDER_Y_RESPONDER = "test_historia_conc_rr";
    private static final String RESPONDER_Y_SALTEAR = "test_historia_conc_rs";
    private static final String SALTEAR_Y_SALTEAR = "test_historia_conc_ss";
    private static final List<String> USUARIOS = List.of(RESPONDER_Y_RESPONDER, RESPONDER_Y_SALTEAR, SALTEAR_Y_SALTEAR);

    @Autowired
    private HistoriaDeVidaService historia;
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
    void responderYResponderALaVezDejaUnaSolaRelacion() throws Exception {
        verificar(ParejasConcurrentes.correr("responder + responder", historia, neo4j, personas.crear(RESPONDER_Y_RESPONDER),
                preguntas(), Accion.RESPONDER, Accion.RESPONDER));
    }

    @Test
    void responderYSaltearALaVezDejaUnaSolaRelacion() throws Exception {
        verificar(ParejasConcurrentes.correr("responder + saltear", historia, neo4j, personas.crear(RESPONDER_Y_SALTEAR),
                preguntas(), Accion.RESPONDER, Accion.SALTEAR));
    }

    @Test
    void saltearYSaltearALaVezDejaUnaSolaRelacion() throws Exception {
        verificar(ParejasConcurrentes.correr("saltear + saltear", historia, neo4j, personas.crear(SALTEAR_Y_SALTEAR),
                preguntas(), Accion.SALTEAR, Accion.SALTEAR));
    }

    static List<Integer> preguntas() {
        return IntStream.rangeClosed(1, PARES).boxed().toList();
    }

    private static void verificar(Resumen resumen) {
        System.out.println("[CONCURRENCIA] CON bloqueo, " + resumen);
        assertThat(resumen.errores()).as("errores: %s", resumen.detalleDeErrores()).isZero();
        assertThat(resumen.dosExitos()).as("los dos pedidos quedaron registrados").isZero();
        assertThat(resumen.duplicadas()).as("preguntas con más de una relación").isZero();
        assertThat(resumen.correctos()).as("un registrado y un 409").isEqualTo(resumen.pares());
    }
}
