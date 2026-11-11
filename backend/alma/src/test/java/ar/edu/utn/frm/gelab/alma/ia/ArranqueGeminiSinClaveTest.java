package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * La aplicación completa (con su configuración real) arranca con {@code gemini} y sin clave, sin importar
 * lo que haya en el {@code .env}: el contexto carga, el modelo es el de Gemini y falla en cada pedido
 * (contrato de IA, sección 2, "Sin clave"). Es el {@code contextLoads} de ese caso.
 */
@SpringBootTest(properties = { "alma.ia.proveedor=gemini", "alma.ia.gemini.api-key=", "alma.seed.enabled=false" })
class ArranqueGeminiSinClaveTest {

    @Autowired
    private ModeloConversacional modelo;

    @Autowired
    private ObjectProvider<GoogleGenAiChatModel> chatModel;

    @Test
    void elContextoCargaYElModeloFallaSinLlamarALaRed() {
        assertInstanceOf(GeminiModeloConversacional.class, modelo);
        assertNull(chatModel.getIfAvailable());
        assertThrows(ModeloNoDisponibleException.class, () -> modelo.responder("I",
                List.of(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Hola"))));
    }
}
