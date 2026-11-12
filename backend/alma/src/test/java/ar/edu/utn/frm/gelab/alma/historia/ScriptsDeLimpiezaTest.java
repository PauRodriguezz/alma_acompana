package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;
import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.filas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.domain.TipoProgreso;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Los scripts de {@code backend/alma/scripts/} que limpian el recuerdo de prueba "Los tiempos de la adolescencia"
 * (vista previa y borrado con cambio a salteo), ejecutados de verdad contra el Neo4j de {@code docker compose up -d}.
 * Solo tocan nodos de cuentas {@code test_historia_scripts_*}: el script de borrado va fijado a una persona y a un
 * recuerdo, así que no puede alcanzar datos de desarrollo. Pregunta 10 del guion real.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@Import(ModeloDePruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class ScriptsDeLimpiezaTest {

    private static final Path VISTA_PREVIA = Path.of("scripts", "3-vista-previa-recuerdo-adolescencia.cypher");
    private static final Path BORRADO = Path.of("scripts", "4-borrar-recuerdo-adolescencia-y-saltear.cypher");
    private static final String TITULO = "Los tiempos de la adolescencia";
    private static final String USUARIO_A = "test_historia_scripts_a";
    private static final String USUARIO_B = "test_historia_scripts_b";
    private static final List<String> USUARIOS = List.of(USUARIO_A, USUARIO_B);
    private static final int PREGUNTA = 10;
    private static final int OTRA_PREGUNTA = 11;

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

    private PersonasDeHistoria personas;
    private RecuerdosDePrueba recuerdos;
    private Persona personaA;
    private Persona personaB;
    private int minutos;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        recuerdos = new RecuerdosDePrueba(neo4j);
        USUARIOS.forEach(personas::borrar);
        personaA = personas.crear(USUARIO_A);
        personaB = personas.crear(USUARIO_B);
        minutos = 0;
    }

    @AfterEach
    void limpiar() {
        USUARIOS.forEach(personas::borrar);
    }

    // ---------- Armado ----------

    private String recuerdoRespondido(Persona quien, int pregunta, String titulo) {
        escritura.registrarProgreso(quien.personaId(), pregunta, TipoProgreso.RESPONDIO);
        return recuerdos.crear(quien.personaId(), pregunta, titulo, null, null, 0, 0, 0, minutos++);
    }

    /** La sentencia del script: sin las líneas de comentario y sin el punto y coma final. */
    private static String sentencia(Path script) throws IOException {
        String texto = String.join("\n", Files.readAllLines(script, StandardCharsets.UTF_8).stream()
                .filter(linea -> !linea.trim().startsWith("//")).toList()).strip();
        assertThat(texto).endsWith(";");
        return texto.substring(0, texto.length() - 1);
    }

    private Collection<Map<String, Object>> correrBorrado(Persona quien, String recuerdoId) throws IOException {
        return neo4j.query(sentencia(BORRADO)).bindAll(Map.of("persona", quien.personaId(), "recuerdo", recuerdoId))
                .fetch().all();
    }

    private long relaciones(Persona quien, int pregunta, String tipo) {
        return contar(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:" + tipo
                + "]->(:PreguntaGuion {idPreguntaGuion: $q}) RETURN count(r) AS c", "p", quien.personaId(), "q", pregunta);
    }

    private long recuerdosDe(String recuerdoId) {
        return contar(neo4j, "MATCH (r:Recuerdo {idRecuerdo: $id}) RETURN count(r) AS c", "id", recuerdoId);
    }

    private Object fechaDe(Persona quien, int pregunta, String tipo) {
        return filas(neo4j, "MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:" + tipo
                + "]->(:PreguntaGuion {idPreguntaGuion: $q}) RETURN r.fecha AS fecha", "p", quien.personaId(), "q",
                pregunta).get(0).get("fecha");
    }

    // ---------- Vista previa ----------

    @Test
    void laVistaPreviaListaElRecuerdoConSusDatosYNoModificaNada() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        recuerdos.agregarLugar(id, "Allen");
        recuerdoRespondido(personaA, OTRA_PREGUNTA, "Otro recuerdo");

        List<Map<String, Object>> filas = neo4j.query(sentencia(VISTA_PREVIA)).fetch().all().stream()
                .filter(f -> f.get("idPersonaMayor").equals(personaA.personaId())).toList();

        assertThat(filas).hasSize(1);
        Map<String, Object> fila = filas.get(0);
        assertThat(fila).containsEntry("pregunta", (long) PREGUNTA).containsEntry("idRecuerdo", id)
                .containsEntry("tieneRespondio", true).containsEntry("tieneSalteo", false)
                .containsEntry("tienePersonas", false).containsEntry("tieneLugares", true);
        assertThat(fila.get("descripcion")).isEqualTo("Descripción original.");
        assertThat(recuerdosDe(id)).as("la vista previa no borra nada").isEqualTo(1);
        assertThat(relaciones(personaA, PREGUNTA, "RESPONDIO")).isEqualTo(1);
    }

    @Test
    void laVistaPreviaNoListaLosRecuerdosConOtroTitulo() throws IOException {
        recuerdoRespondido(personaA, PREGUNTA, "Otro título");

        long deA = neo4j.query(sentencia(VISTA_PREVIA)).fetch().all().stream()
                .filter(f -> f.get("idPersonaMayor").equals(personaA.personaId())).count();

        assertThat(deA).isZero();
    }

    // ---------- Borrado y salteo ----------

    @Test
    void borraElRecuerdoYCambiaLaRelacionRespondioPorSalteoConLaMismaFecha() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        Object fechaOriginal = fechaDe(personaA, PREGUNTA, "RESPONDIO");

        Collection<Map<String, Object>> resultado = correrBorrado(personaA, id);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.iterator().next()).containsEntry("recuerdosBorrados", 1L)
                .containsEntry("relacionesCambiadas", 1L).containsEntry("personasHuerfanasBorradas", 0L)
                .containsEntry("lugaresHuerfanosBorrados", 0L);
        assertThat(recuerdosDe(id)).isZero();
        assertThat(relaciones(personaA, PREGUNTA, "RESPONDIO")).isZero();
        assertThat(relaciones(personaA, PREGUNTA, "SALTEO")).isEqualTo(1);
        assertThat(fechaDe(personaA, PREGUNTA, "SALTEO")).isEqualTo(fechaOriginal);
    }

    @Test
    void despuesDelBorradoLaVistaPreviaYaNoLoListaYLaSegundaVezNoHaceNada() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        correrBorrado(personaA, id);

        assertThat(neo4j.query(sentencia(VISTA_PREVIA)).fetch().all().stream()
                .filter(f -> f.get("idPersonaMayor").equals(personaA.personaId())).count())
                .as("la vista previa devuelve cero filas de esta persona").isZero();
        assertThat(correrBorrado(personaA, id)).as("sin filas: no había nada que borrar").isEmpty();
        assertThat(relaciones(personaA, PREGUNTA, "SALTEO")).isEqualTo(1);
    }

    @Test
    void borraLasPersonasYLosLugaresQueQuedanSinOtroRecuerdoPeroNoLosQueSiguenEnUso() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        recuerdos.agregarLugar(id, "Lugar solo de este recuerdo");
        recuerdos.agregarPersona(personaA.personaId(), id, "Marta", "amiga");
        recuerdos.agregarPersona(personaA.personaId(), id, "Rosa", "madre");
        String otro = recuerdoRespondido(personaA, OTRA_PREGUNTA, "Otro recuerdo");
        // Rosa también participa del otro recuerdo: tiene que quedar.
        neo4j.query("""
                MATCH (pe:Persona {nombre: 'Rosa'})-[:PARTICIPA_EN]->(:Recuerdo {idRecuerdo: $a})
                MATCH (r2:Recuerdo {idRecuerdo: $b})
                CREATE (pe)-[:PARTICIPA_EN]->(r2)
                """).bindAll(Map.of("a", id, "b", otro)).run();

        Map<String, Object> fila = correrBorrado(personaA, id).iterator().next();

        assertThat(fila).containsEntry("personasHuerfanasBorradas", 1L).containsEntry("lugaresHuerfanosBorrados", 1L);
        assertThat(contar(neo4j, "MATCH (pe:Persona {nombre: 'Marta'})<-[:CONOCE]-(:PersonaMayor {idPersonaMayor: $p}) "
                + "RETURN count(pe) AS c", "p", personaA.personaId())).as("Marta se borró").isZero();
        assertThat(contar(neo4j, "MATCH (pe:Persona {nombre: 'Rosa'})-[:PARTICIPA_EN]->(:Recuerdo {idRecuerdo: $b}) "
                + "RETURN count(pe) AS c", "b", otro)).as("Rosa sigue en el otro recuerdo").isEqualTo(1);
        assertThat(recuerdosDe(otro)).isEqualTo(1);
    }

    @Test
    void noTocaOtrosRecuerdosDeLaMismaPersonaNiLosDeOtraPersonaConElMismoTitulo() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        String otro = recuerdoRespondido(personaA, OTRA_PREGUNTA, "Otro recuerdo");
        String deB = recuerdoRespondido(personaB, PREGUNTA, TITULO);

        correrBorrado(personaA, id);

        assertThat(recuerdosDe(otro)).isEqualTo(1);
        assertThat(relaciones(personaA, OTRA_PREGUNTA, "RESPONDIO")).isEqualTo(1);
        assertThat(recuerdosDe(deB)).as("el de B no se toca").isEqualTo(1);
        assertThat(relaciones(personaB, PREGUNTA, "RESPONDIO")).isEqualTo(1);
        assertThat(relaciones(personaB, PREGUNTA, "SALTEO")).isZero();
    }

    @Test
    void conLaPersonaEquivocadaOConOtroTituloNoBorraNada() throws IOException {
        String conTitulo = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        String otroTitulo = recuerdoRespondido(personaA, OTRA_PREGUNTA, "Otro título");

        assertThat(correrBorrado(personaB, conTitulo)).as("recuerdo de otra persona").isEmpty();
        assertThat(correrBorrado(personaA, otroTitulo)).as("recuerdo con otro título").isEmpty();

        assertThat(recuerdosDe(conTitulo)).isEqualTo(1);
        assertThat(recuerdosDe(otroTitulo)).isEqualTo(1);
        assertThat(relaciones(personaA, PREGUNTA, "RESPONDIO")).isEqualTo(1);
        assertThat(relaciones(personaA, OTRA_PREGUNTA, "RESPONDIO")).isEqualTo(1);
    }

    @Test
    void sinLosParametrosLaConsultaFallaYNoBorraNada() throws IOException {
        String id = recuerdoRespondido(personaA, PREGUNTA, TITULO);
        String consulta = sentencia(BORRADO);

        assertThatThrownBy(() -> neo4j.query(consulta).fetch().all()).isNotNull();

        assertThat(recuerdosDe(id)).isEqualTo(1);
        assertThat(relaciones(personaA, PREGUNTA, "RESPONDIO")).isEqualTo(1);
    }

    @Test
    void unRecuerdoSinRespondioIgualQuedaSalteadoYNoDaError() throws IOException {
        String id = recuerdos.crear(personaA.personaId(), PREGUNTA, TITULO, null, null, 0, 0, 0, minutos++);

        Map<String, Object> fila = correrBorrado(personaA, id).iterator().next();

        assertThat(fila).containsEntry("recuerdosBorrados", 1L).containsEntry("relacionesCambiadas", 0L);
        assertThat(relaciones(personaA, PREGUNTA, "SALTEO")).isEqualTo(1);
    }

    @Test
    void losDosScriptsTienenLaAdvertenciaDeSoloParaDesarrollo() throws IOException {
        for (Path script : List.of(VISTA_PREVIA, BORRADO)) {
            assertThat(Files.readString(script, StandardCharsets.UTF_8)).contains("SOLO PARA DESARROLLO");
        }
    }
}
