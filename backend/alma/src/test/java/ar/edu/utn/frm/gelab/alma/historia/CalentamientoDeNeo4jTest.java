package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.neo4j.core.Neo4jClient;

import ar.edu.utn.frm.gelab.alma.historia.config.CalentamientoDeNeo4j;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;

/**
 * El calentamiento de Neo4j al arrancar: con Neo4j real no escribe nada ni crea un {@code PersonaMayor} falso, y ante
 * una falla no rompe nada y no registra el mensaje del error. En las pruebas el calentamiento automático está
 * apagado ({@code src/test/resources/config/application.properties}); acá se llama a {@code calentar()} a mano.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class CalentamientoDeNeo4jTest {

    @Autowired
    private Driver driver;
    @Autowired
    private LecturaDelGrafo lectura;
    @Autowired
    private Neo4jClient neo4jClient;

    private long contar(String cypher) {
        return neo4jClient.query(cypher).fetchAs(Long.class).one().orElseThrow();
    }

    @Test
    void conNeo4jRealCalientaSinCrearNiModificarNada(CapturedOutput salida) {
        long nodosAntes = contar("MATCH (n) RETURN count(n)");
        long relacionesAntes = contar("MATCH ()-[r]->() RETURN count(r)");

        assertDoesNotThrow(() -> new CalentamientoDeNeo4j(driver, lectura).calentar());

        assertThat(contar("MATCH (n) RETURN count(n)")).as("nodos").isEqualTo(nodosAntes);
        assertThat(contar("MATCH ()-[r]->() RETURN count(r)")).as("relaciones").isEqualTo(relacionesAntes);
        assertThat(contar("MATCH (p:PersonaMayor {idPersonaMayor: -1}) RETURN count(p)"))
                .as("no existe un PersonaMayor con id -1").isZero();
        assertThat(salida.getOut() + salida.getErr()).contains("Neo4j calentado en");
    }

    @Test
    void siNeo4jFallaNoLanzaNadaYNoRegistraElMensajeDelError(CapturedOutput salida) {
        Driver caido = mock(Driver.class);
        LecturaDelGrafo lecturaSinUso = mock(LecturaDelGrafo.class);
        doThrow(new ServiceUnavailableException("MENSAJE-SECRETO-DEL-DRIVER")).when(caido).verifyConnectivity();

        assertDoesNotThrow(() -> new CalentamientoDeNeo4j(caido, lecturaSinUso).calentar());

        String log = salida.getOut() + salida.getErr();
        assertThat(log).contains("No se pudo calentar la conexión a Neo4j").contains("ServiceUnavailableException")
                .doesNotContain("MENSAJE-SECRETO-DEL-DRIVER");
        verifyNoInteractions(lecturaSinUso);
    }

    /**
     * Qué hace el driver real con la marca de interrupción puesta antes de la consulta (lo que deja
     * {@code cancel(true)}): lanza una excepción de acceso a datos ({@code TransientDataAccessResourceException},
     * con {@code ServiceUnavailableException} de causa) y la marca <b>sigue puesta</b>. Cancelar en medio de una
     * consulta puede dar otra excepción (en la prueba real apareció {@code InvalidDataAccessResourceUsageException})
     * y no se garantiza que la marca llegue al {@code catch}: por eso {@code RecuerdosParaElChat} usa una bandera
     * propia y no depende de ninguna de las dos cosas.
     */
    @Test
    void conElHiloInterrumpidoLaLecturaRealLanzaUnaExcepcionDeAccesoADatos() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(org.springframework.dao.DataAccessException.class, () -> lectura.estadoDeTodas(-1L));
        } finally {
            Thread.interrupted(); // no dejar la marca puesta para otras pruebas
        }
    }
}
