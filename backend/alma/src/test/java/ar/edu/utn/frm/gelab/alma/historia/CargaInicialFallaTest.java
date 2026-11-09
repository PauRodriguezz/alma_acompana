package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import ar.edu.utn.frm.gelab.alma.historia.config.CargaInicialDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import tools.jackson.databind.json.JsonMapper;

/**
 * Con Neo4j caído al arrancar (contrato del grafo, sección 12): la carga inicial registra un ERROR y no
 * lanza nada hacia afuera, para que el resto de la aplicación arranque. No necesita Neo4j: se simula la caída.
 */
@ExtendWith(OutputCaptureExtension.class)
class CargaInicialFallaTest {

    @Test
    void siNeo4jNoEstaRegistraUnErrorYNoLanzaNada(CapturedOutput salida) {
        EscrituraDelGrafo escritura = mock(EscrituraDelGrafo.class);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("Unable to connect a bolt://secreto:7687"))
                .when(escritura).crearRestricciones(anyList());
        CargaInicialDelGrafo carga = new CargaInicialDelGrafo(escritura, JsonMapper.builder().build(), null, null, true);

        assertThatCode(carga::cargar).doesNotThrowAnyException();

        verify(escritura, never()).crearPreguntasFaltantes(anyList());
        verify(escritura, never()).cargarHistoriaDeEjemplo(org.mockito.ArgumentMatchers.anyLong());
        assertThat(salida.getAll()).contains("No se pudo cargar el grafo inicial (DataAccessResourceFailureException)")
                .contains("próximo arranque")
                // Solo el tipo de error: el mensaje de la excepción no se escribe en el log.
                .doesNotContain("bolt://secreto");
    }

    @Test
    void siFallaLaCargaDelGuionTampocoLanzaNada(CapturedOutput salida) {
        EscrituraDelGrafo escritura = mock(EscrituraDelGrafo.class);
        when(escritura.crearPreguntasFaltantes(anyList())).thenThrow(new IllegalStateException("falla"));
        CargaInicialDelGrafo carga = new CargaInicialDelGrafo(escritura, JsonMapper.builder().build(), null, null, true);

        assertThatCode(carga::cargar).doesNotThrowAnyException();

        verify(escritura, never()).cargarHistoriaDeEjemplo(org.mockito.ArgumentMatchers.anyLong());
        assertThat(salida.getAll()).contains("No se pudo cargar el grafo inicial (IllegalStateException)");
    }
}
