package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Si falla la lectura del grafo para el prompt (por ejemplo, con Neo4j caído), el chat no se rompe: responde 200 y
 * el prompt dice "No hay información." (contrato de historia de vida, 5.5.2). El log lleva solo el tipo de error,
 * nunca el mensaje de la persona. Contexto propio: reemplaza {@link LecturaDelGrafo} por un doble que falla.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(ModeloDePruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RecuerdosEnElChatFallaTest {

    private static final String USUARIO = "test_historia_chat_falla";
    private static final String MENSAJE_SECRETO = "Hoy recordé a Elena y a mi abuela Rosa";

    @MockitoBean
    private LecturaDelGrafo lectura;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HistoriaDeVidaService historia;
    @Autowired
    private ModeloDePrueba modelo;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtEncoder jwtEncoder;

    private PersonasDeHistoria personas;
    private String token;

    @BeforeEach
    void preparar() {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        personas.borrar(USUARIO);
        modelo.reiniciar();
        Persona persona = personas.crear(USUARIO);
        token = personas.token(persona.cuentaId(), "PERSONA_MAYOR");
        RuntimeException caido = new IllegalStateException("Neo4j caído");
        when(lectura.estadoDeTodas(anyLong())).thenThrow(caido);
        when(lectura.personasConocidas(anyLong())).thenThrow(caido);
        when(lectura.lugaresConocidos(anyLong())).thenThrow(caido);
        when(lectura.recuerdosDeEtapa(anyLong(), any())).thenThrow(caido);
    }

    @AfterEach
    void limpiar() {
        personas.borrar(USUARIO);
        modelo.reiniciar();
    }

    @Test
    void conElGrafoCaidoElChatRespondeIgualYElPromptDiceNoHayInformacion(CapturedOutput salida) throws Exception {
        mockMvc.perform(post("/api/chat/mensajes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"" + MENSAJE_SECRETO + " de chica\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").isNotEmpty());

        List<ModeloDePrueba.Pedido> pedidos = modelo.pedidos();
        assertThat(pedidos).hasSize(1);
        String prompt = pedidos.get(0).instrucciones();
        assertThat(prompt).contains("Historia de vida: No hay información.")
                .contains("Recuerdos relacionados: No hay información.");

        // Se registró el error (solo el tipo) y nada del mensaje.
        assertThat(salida.getAll()).contains("No se pudo leer").contains("IllegalStateException")
                .doesNotContain("Elena").doesNotContain("Rosa").doesNotContain("Neo4j caído");
    }
}
