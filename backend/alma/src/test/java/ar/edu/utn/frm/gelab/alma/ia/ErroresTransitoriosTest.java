package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;

/** Qué errores de cada proveedor se consideran transitorios (contrato del grafo, 5.4, paso 6). Sin red. */
class ErroresTransitoriosTest {

    @Test
    void geminiUnServerExceptionConCodigoDeServidorEsTransitorio() {
        for (int codigo : new int[] { 500, 502, 503, 504 }) {
            assertTrue(ErroresTransitorios.deGemini(new ServerException(codigo, "UNAVAILABLE", "x")), "código " + codigo);
        }
    }

    @Test
    void geminiUn429EsTransitorioPeroOtrosCodigosDeClienteNo() {
        assertTrue(ErroresTransitorios.deGemini(new ClientException(429, "RESOURCE_EXHAUSTED", "x")));
        for (int codigo : new int[] { 400, 401, 402, 403, 404 }) {
            assertFalse(ErroresTransitorios.deGemini(new ClientException(codigo, "X", "x")), "código " + codigo);
        }
    }

    @Test
    void geminiBuscaElErrorDeLaApiEnLaCadenaDeCausas() {
        RuntimeException envuelto = new RuntimeException("envuelto",
                new IllegalStateException("otro", new ServerException(503, "UNAVAILABLE", "x")));

        assertTrue(ErroresTransitorios.deGemini(envuelto));
    }

    @Test
    void geminiUnCortePorTiempoOUnaFallaDeRedNoSonTransitorios() {
        assertFalse(ErroresTransitorios.deGemini(new RuntimeException(new SocketTimeoutException("timeout"))));
        assertFalse(ErroresTransitorios.deGemini(new RuntimeException(new IOException("reset"))));
        assertFalse(ErroresTransitorios.deGemini(new IllegalStateException("sin causa")));
    }

    @Test
    void ollamaSoloEsTransitorioElTransientAiException() {
        assertTrue(ErroresTransitorios.deOllama(new TransientAiException("503")));
        assertTrue(ErroresTransitorios.deOllama(new RuntimeException("envuelto", new TransientAiException("503"))));
        assertFalse(ErroresTransitorios.deOllama(new NonTransientAiException("400")));
        assertFalse(ErroresTransitorios.deOllama(new RuntimeException(new SocketTimeoutException("timeout"))));
    }

    @Test
    void lasExcepcionesSinIndicadorNoSonTransitorias() {
        assertFalse(new ModeloNoDisponibleException("falla").esTransitorio());
        assertFalse(new ModeloNoDisponibleException("falla", new RuntimeException()).esTransitorio());
        assertTrue(new ModeloNoDisponibleException("falla", new RuntimeException(), true).esTransitorio());
    }
}
