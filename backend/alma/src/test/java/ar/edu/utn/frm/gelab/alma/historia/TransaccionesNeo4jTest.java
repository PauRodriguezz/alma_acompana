package ar.edu.utn.frm.gelab.alma.historia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.Repository;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.repository.PreguntaGuionRepository;

/**
 * Prueba permanente de la configuración de transacciones con JPA y Neo4j a la vez (contrato del grafo,
 * sección 2, "Transacciones", y checklist, punto 1). Usa el Postgres y el Neo4j de {@code docker compose up -d}.
 * Los nodos de prueba llevan la etiqueta {@code PruebaTx} (no es del modelo) y las cuentas el prefijo
 * {@code test_tx_}; se borran antes y después de cada prueba. Imprime los hechos con el prefijo [TX].
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
// Contexto propio (sondas de prueba, y necesita ver el arranque): se cierra al terminar para liberar su pool.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransaccionesNeo4jTest {

    private static final String PREFIJO = "[TX] ";
    private static final String MANEJADOR_NEO4J = "neo4jTransactionManager";
    private static final String USUARIO_OK = "test_tx_ok";
    private static final String USUARIO_FALLA = "test_tx_falla";
    private static final String USUARIO_DOS_BASES = "test_tx_dos_bases";
    private static final String[] USUARIOS = { USUARIO_OK, USUARIO_FALLA, USUARIO_DOS_BASES };

    /** Los servicios con {@code @Transactional} sin calificar que existen antes del módulo de historia. */
    private static final Set<String> SERVICIOS_DE_JPA = Set.of("AccesibilidadService", "AuthService",
            "DatosDeCatalogo", "DatosDeCatalogoJuegos", "DatosDeCatalogoMicrocapsulas", "DatosDePrueba",
            "EjercicioService", "JuegoService", "LecturaMicrocapsulaService", "MicrocapsulaService",
            "PanelAdminService", "PerfilService", "RecordatorioService", "RegistroAnimoService");

    /** Una clase con {@code @Transactional} sin calificar, como los servicios del proyecto. */
    public static class SondaJpa {

        private final CuentaRepository cuentaRepository;

        public SondaJpa(CuentaRepository cuentaRepository) {
            this.cuentaRepository = cuentaRepository;
        }

        @Transactional
        public void guardar(String nombreUsuario, boolean fallar) {
            cuentaRepository.save(cuenta(nombreUsuario));
            if (fallar) {
                throw new IllegalStateException("falla a propósito, a la mitad");
            }
        }
    }

    /** Escrituras al grafo con el manejador de Neo4j, sin calificar (control) y con las dos bases. */
    public static class SondaNeo4j {

        private final Neo4jClient neo4jClient;
        private final CuentaRepository cuentaRepository;
        private final PreguntaGuionRepository preguntaRepository;

        public SondaNeo4j(Neo4jClient neo4jClient, CuentaRepository cuentaRepository,
                PreguntaGuionRepository preguntaRepository) {
            this.neo4jClient = neo4jClient;
            this.cuentaRepository = cuentaRepository;
            this.preguntaRepository = preguntaRepository;
        }

        @Transactional(MANEJADOR_NEO4J)
        public void escribirDosNodos(String marca, boolean fallarALaMitad) {
            crearNodo(marca, 1);
            if (fallarALaMitad) {
                throw new IllegalStateException("falla a propósito, a la mitad");
            }
            crearNodo(marca, 2);
        }

        /** Control: el mismo trabajo, sin ninguna transacción de Spring. */
        public void escribirDosNodosSinTransaccion(String marca, boolean fallarALaMitad) {
            crearNodo(marca, 1);
            if (fallarALaMitad) {
                throw new IllegalStateException("falla a propósito, a la mitad");
            }
            crearNodo(marca, 2);
        }

        /** El mismo trabajo, con el manejador por defecto (el de JPA) y no con el de Neo4j. */
        @Transactional
        public void escribirDosNodosConElManejadorDeJpa(String marca, boolean fallarALaMitad) {
            crearNodo(marca, 1);
            if (fallarALaMitad) {
                throw new IllegalStateException("falla a propósito, a la mitad");
            }
            crearNodo(marca, 2);
        }

        @Transactional(MANEJADOR_NEO4J)
        public void escribirEnLasDosBases(String marca, String nombreUsuario) {
            crearNodo(marca, 1);
            cuentaRepository.save(cuenta(nombreUsuario));
            throw new IllegalStateException("falla a propósito, después de escribir en las dos bases");
        }

        /**
         * Crea una pregunta de prueba con {@code Neo4jClient} y la lee con el repositorio real, todo antes de
         * terminar la transacción. El repositorio solo ve la escritura sin confirmar si se une a la misma
         * transacción de Neo4j.
         *
         * @return si el repositorio vio la pregunta recién creada
         */
        @Transactional(MANEJADOR_NEO4J)
        public boolean crearYLeerConElRepositorio(int id, boolean fallar) {
            neo4jClient.query("CREATE (:PreguntaGuion {idPreguntaGuion: $id, orden: $id, texto: 'Pregunta de prueba'})")
                    .bind(id).to("id").run();
            boolean laVe = preguntaRepository.findById(id).isPresent();
            if (fallar) {
                throw new IllegalStateException("falla a propósito, después de escribir y leer con el repositorio");
            }
            return laVe;
        }

        private void crearNodo(String marca, int n) {
            neo4jClient.query("CREATE (:PruebaTx {marca: $marca, n: $n})")
                    .bind(marca).to("marca").bind(n).to("n").run();
        }
    }

    @TestConfiguration
    static class Configuracion {
        @Bean
        SondaJpa sondaJpa(CuentaRepository cuentaRepository) {
            return new SondaJpa(cuentaRepository);
        }

        @Bean
        SondaNeo4j sondaNeo4j(Neo4jClient neo4jClient, CuentaRepository cuentaRepository,
                PreguntaGuionRepository preguntaRepository) {
            return new SondaNeo4j(neo4jClient, cuentaRepository, preguntaRepository);
        }
    }

    @Autowired
    private ApplicationContext contexto;
    @Autowired
    private SondaJpa sondaJpa;
    @Autowired
    private SondaNeo4j sondaNeo4j;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PreguntaGuionRepository preguntaRepository;
    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    @AfterEach
    void limpiar() {
        for (String usuario : USUARIOS) {
            cuentaRepository.findByNombreUsuario(usuario).ifPresent(cuentaRepository::delete);
        }
        neo4jClient.query("MATCH (n:PruebaTx) DETACH DELETE n").run();
        GrafoDePrueba.borrarPreguntasDePrueba(neo4jClient);
    }

    // ---------- Manejadores y servicios existentes ----------

    @Test
    void hayDosManejadoresYElDeJpaEsElPrimario() {
        Map<String, TransactionManager> manejadores = new TreeMap<>(contexto.getBeansOfType(TransactionManager.class));
        manejadores.forEach((nombre, manejador) -> System.out.println(
                PREFIJO + "manejador: bean '" + nombre + "' -> " + manejador.getClass().getName()));

        assertEquals(Set.of("transactionManager", MANEJADOR_NEO4J), manejadores.keySet());
        assertInstanceOf(JpaTransactionManager.class, manejadores.get("transactionManager"));
        assertInstanceOf(Neo4jTransactionManager.class, manejadores.get(MANEJADOR_NEO4J));

        TransactionManager sinCalificar = contexto.getBean(TransactionManager.class);
        System.out.println(PREFIJO + "el que se usa sin calificar: " + sinCalificar.getClass().getName());
        assertInstanceOf(JpaTransactionManager.class, sinCalificar);
    }

    @Test
    void losServiciosDeSiempreSiguenUsandoJpa() {
        AnnotationTransactionAttributeSource fuente = new AnnotationTransactionAttributeSource();
        TreeSet<String> sinCalificar = new TreeSet<>();
        TreeSet<String> calificados = new TreeSet<>();
        for (String nombre : contexto.getBeanDefinitionNames()) {
            Class<?> clase = AopUtils.getTargetClass(contexto.getBean(nombre));
            if (!clase.getName().startsWith("ar.edu.utn.frm.gelab.alma.") || clase.getName().contains("Sonda")
                    || clase.getName().contains("Test")) {
                continue;
            }
            boolean transaccional = false;
            boolean conCalificador = false;
            for (Method metodo : clase.getMethods()) {
                TransactionAttribute atributo = fuente.getTransactionAttribute(metodo, clase);
                if (atributo != null) {
                    transaccional = true;
                    conCalificador |= atributo.getQualifier() != null && !atributo.getQualifier().isEmpty();
                }
            }
            if (transaccional) {
                (conCalificador ? calificados : sinCalificar).add(clase.getSimpleName());
            }
        }
        System.out.println(PREFIJO + "clases con @Transactional sin calificar (" + sinCalificar.size() + "): "
                + sinCalificar);
        System.out.println(PREFIJO + "clases con @Transactional calificado (" + calificados.size() + "): "
                + calificados);

        assertTrue(sinCalificar.containsAll(SERVICIOS_DE_JPA), "Faltan servicios de JPA: " + sinCalificar);
        // Solo el módulo de historia puede calificar el manejador (y todavía no tiene servicios).
        calificados.forEach(clase -> assertTrue(contexto.getBeansOfType(Object.class).values().stream()
                .map(AopUtils::getTargetClass)
                .anyMatch(c -> c.getSimpleName().equals(clase)
                        && c.getName().startsWith("ar.edu.utn.frm.gelab.alma.historia.")),
                "Una clase fuera de historia califica el manejador: " + clase));
    }

    @Test
    void unTransactionalSinCalificarDeshaceEnPostgres() {
        assertThrows(IllegalStateException.class, () -> sondaJpa.guardar(USUARIO_FALLA, true));
        boolean quedo = cuentaRepository.findByNombreUsuario(USUARIO_FALLA).isPresent();
        System.out.println(PREFIJO + "tras fallar a la mitad, la cuenta quedó en PostgreSQL: " + quedo);
        assertFalse(quedo, "La escritura de JPA tendría que haberse deshecho");

        sondaJpa.guardar(USUARIO_OK, false);
        boolean confirmada = cuentaRepository.findByNombreUsuario(USUARIO_OK).isPresent();
        System.out.println(PREFIJO + "sin fallar, la cuenta quedó confirmada en PostgreSQL: " + confirmada);
        assertTrue(confirmada);
    }

    // ---------- Escrituras a Neo4j ----------

    @Test
    void unaEscrituraAlGrafoSeDeshaceSiFallaALaMitad() {
        String marca = marca();
        assertThrows(IllegalStateException.class, () -> sondaNeo4j.escribirDosNodos(marca, true));
        long nodos = contar(marca);
        System.out.println(PREFIJO + "falla a la mitad con neo4jTransactionManager: nodos que quedaron = " + nodos);
        assertEquals(0, nodos, "Con el manejador de Neo4j nada debería quedar");
    }

    @Test
    void unaEscrituraAlGrafoSinFallarSeConfirma() {
        String marca = marca();
        sondaNeo4j.escribirDosNodos(marca, false);
        long nodos = contar(marca);
        System.out.println(PREFIJO + "sin fallar con neo4jTransactionManager: nodos que quedaron = " + nodos);
        assertEquals(2, nodos);
    }

    @Test
    void controlSinTransaccionLaEscrituraNoSeDeshace() {
        String marca = marca();
        assertThrows(IllegalStateException.class, () -> sondaNeo4j.escribirDosNodosSinTransaccion(marca, true));
        long nodos = contar(marca);
        System.out.println(PREFIJO + "CONTROL, falla a la mitad sin ninguna transaccion: nodos que quedaron = " + nodos);
        assertEquals(1, nodos, "Sin transacción el primer nodo ya está confirmado: así la prueba detecta el problema");
    }

    /**
     * Hallazgo: Spring Data Neo4j se engancha (Neo4jSessionSynchronization) a la transacción de otro manejador
     * que esté activa. Con un {@code @Transactional} de JPA, el grafo también se deshace si el método falla; lo
     * que no hay es una transacción común (el commit de Neo4j ocurre después del de JPA). El módulo de
     * historia igual usa el manejador propio de Neo4j, que no depende de ese enganche.
     */
    @Test
    void conElManejadorDeJpaElGrafoSeDeshacePorSincronizacion() {
        String marca = marca();
        assertThrows(IllegalStateException.class, () -> sondaNeo4j.escribirDosNodosConElManejadorDeJpa(marca, true));
        long nodos = contar(marca);
        System.out.println(PREFIJO + "falla a la mitad con el manejador de JPA: nodos que quedaron = " + nodos);
        assertEquals(0, nodos);
    }

    @Test
    void sinTransaccionComunSeDeshaceNeo4jYQuedaPostgres() {
        String marca = marca();
        assertThrows(IllegalStateException.class, () -> sondaNeo4j.escribirEnLasDosBases(marca, USUARIO_DOS_BASES));
        long nodos = contar(marca);
        boolean cuenta = cuentaRepository.findByNombreUsuario(USUARIO_DOS_BASES).isPresent();
        System.out.println(PREFIJO + "dos bases, falla al final: nodos en Neo4j = " + nodos
                + ", cuenta en PostgreSQL = " + cuenta);
        assertEquals(0, nodos, "Neo4j se deshace");
        assertTrue(cuenta, "PostgreSQL queda: no hay transacción común (contrato, sección 2)");
    }

    // ---------- Repositorio de Neo4j ----------

    @Test
    void elRepositorioDeNeo4jUsaElManejadorDeNeo4j() {
        TransactionInterceptor interceptor = Arrays.stream(((Advised) preguntaRepository).getAdvisors())
                .map(asesor -> asesor.getAdvice())
                .filter(TransactionInterceptor.class::isInstance)
                .map(TransactionInterceptor.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("El repositorio no tiene interceptor transaccional"));
        // El getter es protected: se lee el campo que fija transactionManagerRef de @EnableNeo4jRepositories.
        Object manejador = ReflectionTestUtils.getField(interceptor, "transactionManagerBeanName");
        System.out.println(PREFIJO + "el repositorio de Neo4j usa el manejador: " + manejador);
        assertEquals(MANEJADOR_NEO4J, manejador);
    }

    /**
     * El repositorio real se une a la transacción de Neo4j: dentro de ella ve una escritura sin confirmar
     * (solo posible si comparte la transacción) y, si el método falla, esa escritura se deshace. Pasa lo que
     * antes comprobaban el nodo y el repositorio de prueba.
     */
    @Test
    void elRepositorioParticipaDeLaTransaccionDeNeo4j() {
        int id = GrafoDePrueba.PRIMERA_PREGUNTA_DE_PRUEBA + 100;
        assertThrows(IllegalStateException.class, () -> sondaNeo4j.crearYLeerConElRepositorio(id, true));
        long conFalla = preguntasDePrueba();
        System.out.println(PREFIJO + "repositorio, falla después de escribir: preguntas que quedaron = " + conFalla);
        assertEquals(0, conFalla, "La escritura tiene que deshacerse junto con la transacción de Neo4j");

        boolean laVio = sondaNeo4j.crearYLeerConElRepositorio(id, false);
        System.out.println(PREFIJO + "repositorio, sin fallar: vio la escritura sin confirmar = " + laVio
                + ", preguntas que quedaron = " + preguntasDePrueba());
        assertTrue(laVio, "El repositorio no vio la escritura sin confirmar: no comparte la transacción");
        assertEquals(1, preguntasDePrueba());
    }

    // ---------- Arranque ----------

    /**
     * Cada repositorio queda en un solo almacén. Spring Data sigue registrando avisos INFO "Could not safely
     * identify store assignment" (el escaneo de JPA ve los repositorios de Neo4j y el escaneo reactivo de
     * Neo4j, que Boot activa porque reactor está en el classpath, ve los de JPA), pero no asigna nada mal:
     * lo que se afirma acá es la asignación real.
     */
    @Test
    void cadaRepositorioQuedaEnUnSoloAlmacen(CapturedOutput salida) {
        String log = salida.getAll();
        long avisos = log.lines().filter(l -> l.contains("Could not safely identify store assignment")).count();
        log.lines().filter(l -> l.contains("Finished Spring Data repository scanning")).forEach(
                l -> System.out.println(PREFIJO + "arranque: " + l.replaceFirst("^.*?--- ", "")));
        System.out.println(PREFIJO + "avisos INFO 'Could not safely identify store assignment': " + avisos);
        assertTrue(log.contains("Bootstrapping Spring Data"),
                "No se capturó el arranque de este contexto: la prueba no puede afirmar nada");
        assertTrue(Pattern.compile("Found [1-9]\\d* Neo4j repository interfaces?\\.").matcher(log).find(),
                "El escaneo de Neo4j (el paquete de ConfiguracionNeo4j) tendría que encontrar sus repositorios");

        TreeSet<String> deNeo4j = new TreeSet<>();
        TreeSet<String> deJpa = new TreeSet<>();
        contexto.getBeansOfType(Repository.class).forEach((nombre, repositorio) -> {
            boolean esNeo4j = repositorio instanceof Neo4jRepository;
            boolean esJpa = repositorio instanceof JpaRepository;
            assertFalse(esNeo4j && esJpa, "El repositorio '" + nombre + "' está en los dos almacenes");
            assertTrue(esNeo4j || esJpa, "El repositorio '" + nombre + "' no está en ningún almacén");
            (esNeo4j ? deNeo4j : deJpa).add(nombre);
            boolean enHistoria = Arrays.stream(((Advised) repositorio).getProxiedInterfaces())
                    .anyMatch(i -> i.getName().startsWith("ar.edu.utn.frm.gelab.alma.historia.repository."));
            assertEquals(esNeo4j, enHistoria,
                    "Solo los repositorios de historia.repository son de Neo4j: " + nombre);
        });
        System.out.println(PREFIJO + "repositorios de Neo4j (" + deNeo4j.size() + "): " + deNeo4j);
        System.out.println(PREFIJO + "repositorios de JPA (" + deJpa.size() + "): " + deJpa);
        assertTrue(deNeo4j.contains("preguntaGuionRepository"));
    }

    private static Cuenta cuenta(String nombreUsuario) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNombreUsuario(nombreUsuario);
        cuenta.setContrasenia("no-se-usa");
        cuenta.setRol(Rol.PERSONA_MAYOR);
        cuenta.setFechaAlta(LocalDate.now());
        return cuenta;
    }

    private static String marca() {
        return UUID.randomUUID().toString();
    }

    private long preguntasDePrueba() {
        return GrafoDePrueba.contar(neo4jClient,
                "MATCH (q:PreguntaGuion) WHERE q.idPreguntaGuion > $desde RETURN count(q) AS c",
                "desde", GrafoDePrueba.PRIMERA_PREGUNTA_DE_PRUEBA);
    }

    private long contar(String marca) {
        return ((Number) neo4jClient.query("MATCH (n:PruebaTx {marca: $marca}) RETURN count(n) AS c")
                .bind(marca).to("marca").fetch().one().orElseThrow().get("c")).longValue();
    }
}
