package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Una lectura lenta del grafo (más larga que el tope de 2 s) no hace esperar al chat: responde 200 dentro de un
 * tiempo razonable y el prompt dice "No hay información." (contrato de historia de vida, 5.5.2; presupuesto de
 * tiempo de {@code RecuerdosParaElChat}). Contexto propio: reemplaza {@link LecturaDelGrafo} por un doble lento que
 * tarda 10 s. Con Neo4j caído por una red que descarta paquetes, el driver tardaría ~21 s por consulta.
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(ModeloDePruebaConfig.class)
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class RecuerdosEnElChatLecturaLentaTest {

    private static final String USUARIO = "test_historia_chat_lenta";
    private static final long LECTURA_LENTA_MS = 10_000;
    private static final String MENSAJE_SECRETO = "Hoy recordé a Elena y a mi abuela Rosa";

    @MockitoBean
    private LecturaDelGrafo lectura;

    @Autowired
    private ConfiguracionHistoria configuracion;
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
    void preparar() throws Exception {
        personas = new PersonasDeHistoria(cuentaRepository, personaMayorRepository, passwordEncoder, historia, jwtEncoder);
        personas.borrar(USUARIO);
        modelo.reiniciar();
        Persona persona = personas.crear(USUARIO);
        token = personas.token(persona.cuentaId(), "PERSONA_MAYOR");
        // La primera lectura del chat (el progreso) se cuelga mucho más que el tope.
        when(lectura.estadoDeTodas(anyLong())).thenAnswer(invocacion -> {
            Thread.sleep(LECTURA_LENTA_MS);
            return List.of();
        });
    }

    @AfterEach
    void limpiar() {
        personas.borrar(USUARIO);
        modelo.reiniciar();
    }

    @Test
    void elTopePorDefectoEsDe2Segundos() {
        assertThat(configuracion.getLecturaChatTimeoutMs()).isEqualTo(2000);
        assertThat(configuracion.getLecturaChatHilosMax()).isEqualTo(4);
    }

    @Test
    void conUnaLecturaMasLentaQueElTopeElChatRespondeIgualYSinEsperarlaTodaYElPromptDiceNoHayInformacion(
            CapturedOutput salida) throws Exception {
        long inicio = System.nanoTime();
        mockMvc.perform(post("/api/chat/mensajes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensaje\":\"" + MENSAJE_SECRETO + " de chica\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").isNotEmpty());
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        // Tope de 2 s: el mensaje no espera los 10 s de la lectura. Margen amplio para una máquina lenta.
        assertThat(tardo).as("tiempo del mensaje").isLessThan(Duration.ofSeconds(6));
        assertThat(modelo.pedidos()).hasSize(1);
        String prompt = modelo.pedidos().get(0).instrucciones();
        assertThat(prompt).contains("Historia de vida: No hay información.")
                .contains("Recuerdos relacionados: No hay información.");
        // Se avisó del tope en el log, sin nada del mensaje.
        assertThat(salida.getAll()).contains("superó el tope de 2000 ms").doesNotContain("Elena")
                .doesNotContain("Rosa");
    }
}
