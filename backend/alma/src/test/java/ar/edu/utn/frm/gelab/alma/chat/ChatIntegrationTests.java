package ar.edu.utn.frm.gelab.alma.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import ar.edu.utn.frm.gelab.alma.ia.TurnoConversacion;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Pruebas del contrato de chat (sección 12, "Endpoints"). Usan el Postgres de Docker ("docker compose up -d").
 * Crean sus propias cuentas (prefijo test_chat_, sin el seed) y las borran al terminar. El modelo es un
 * falso: no necesitan Ollama ni internet. El reloj arranca el 05/11/2026 a las 10:00 (hora de Argentina).
 */
@SpringBootTest(properties = "alma.seed.enabled=false")
@AutoConfigureMockMvc
@Import(ChatIntegrationTests.ConfiguracionDePrueba.class)
@ExtendWith(OutputCaptureExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatIntegrationTests {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 5);
    private static final LocalDateTime INICIO = HOY.atTime(10, 0);
    private static final String USUARIO_A = "test_chat_a";
    private static final String USUARIO_B = "test_chat_b";
    private static final String NOMBRE_A = "Marisol";
    private static final String APELLIDO_A = "Quiroganzo";
    private static final Pattern MARCADOR_SIN_REEMPLAZAR = Pattern.compile("\\{[a-z_]+}");

    private static final String URGENCIA = "Lo que me contás puede ser urgente. Por favor, llamá ahora al 911 o "
            + "pedile a alguien que esté cerca que te ayude. Yo no puedo evaluar lo que te pasa, pero ellos sí.";
    private static final String MALESTAR = "Gracias por contármelo. Lo que sentís es importante y no tenés que "
            + "atravesarlo sin compañía. Podés llamar gratis a la Línea de Salud Mental 0800-999-0091, a "
            + "cualquier hora, o al Centro de Asistencia al Suicida 0800-345-1435. Si estás en peligro, llamá al "
            + "911. Si podés, contáselo también a alguien de tu confianza que esté cerca.";

    /** Reloj con la zona de Argentina cuyo "ahora" se puede mover desde la prueba. */
    static class RelojAjustable extends Clock {
        private final AtomicReference<Instant> instante = new AtomicReference<>();

        void fijar(LocalDateTime fechaHora) {
            instante.set(fechaHora.atZone(ZONA).toInstant());
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instante.get(), zone);
        }

        @Override
        public Instant instant() {
            return instante.get();
        }
    }

    /** Modelo falso: guarda lo que recibe y responde, o falla, según se le pida. */
    static class ModeloFalso implements ModeloConversacional {
        final List<String> instrucciones = new CopyOnWriteArrayList<>();
        final List<List<TurnoConversacion>> turnos = new CopyOnWriteArrayList<>();
        volatile String respuesta = "Respuesta del modelo falso.";
        volatile RuntimeException error;

        @Override
        public String responder(String instruccionesRecibidas, List<TurnoConversacion> turnosRecibidos) {
            instrucciones.add(instruccionesRecibidas);
            turnos.add(new ArrayList<>(turnosRecibidos));
            if (error != null) {
                throw error;
            }
            return respuesta;
        }

        void reiniciar() {
            instrucciones.clear();
            turnos.clear();
            respuesta = "Respuesta del modelo falso.";
            error = null;
        }

        String ultimasInstrucciones() {
            return instrucciones.get(instrucciones.size() - 1);
        }

        List<TurnoConversacion> ultimosTurnos() {
            return turnos.get(turnos.size() - 1);
        }
    }

    @TestConfiguration
    static class ConfiguracionDePrueba {
        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable();
        }

        @Bean
        @Primary
        ModeloFalso modeloFalso() {
            return new ModeloFalso();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private ModeloFalso modelo;

    private Long cuentaA;
    private Long cuentaB;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeAll
    void crearCuentas() {
        cuentaA = cuentaDePrueba(USUARIO_A, NOMBRE_A, APELLIDO_A);
        cuentaB = cuentaDePrueba(USUARIO_B, "Berta", "Zubiaurre");
        tokenA = token(cuentaA, "PERSONA_MAYOR");
        tokenB = token(cuentaB, "PERSONA_MAYOR");
        tokenAdmin = token(999_999L, "ADMINISTRADOR");
    }

    @BeforeEach
    void reiniciar() {
        reloj.fijar(INICIO);
        modelo.reiniciar();
    }

    @BeforeEach
    @AfterEach
    void limpiarDatos() {
        String propias = "select p.id from persona_mayor p where p.cuenta_id in (?, ?)";
        String recordatoriosPropios = "select r.id from recordatorio r where r.persona_mayor_id in (" + propias + ")";
        jdbc.update("delete from registro_animo where persona_mayor_id in (" + propias + ")", cuentaA, cuentaB);
        jdbc.update("delete from notificacion_recordatorio where recordatorio_id in (" + recordatoriosPropios + ")",
                cuentaA, cuentaB);
        jdbc.update("delete from recordatorio_medicacion where id in (" + recordatoriosPropios + ")", cuentaA, cuentaB);
        jdbc.update("delete from recordatorio_cita where id in (" + recordatoriosPropios + ")", cuentaA, cuentaB);
        jdbc.update("delete from recordatorio where persona_mayor_id in (" + propias + ")", cuentaA, cuentaB);
    }

    @AfterAll
    void borrarCuentas() {
        limpiarDatos();
        for (String usuario : List.of(USUARIO_A, USUARIO_B)) {
            cuentaRepository.findByNombreUsuario(usuario).ifPresent(cuenta -> {
                personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(personaMayorRepository::delete);
                cuentaRepository.delete(cuenta);
            });
        }
    }

    // ---------- Acceso ----------

    @Test
    void sinTokenDevuelve401EnLasDosRutas() throws Exception {
        mockMvc.perform(get("/api/chat/saludo"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(post("/api/chat/mensajes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensaje\":\"Hola\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorDevuelve403EnLasDosRutas() throws Exception {
        mockMvc.perform(get("/api/chat/saludo").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden());
        enviar("{\"mensaje\":\"Hola\"}", tokenAdmin).andExpect(status().isForbidden());
        assertTrue(modelo.instrucciones.isEmpty());
    }

    @Test
    void cuentaSinPersonaMayorDevuelve404EnLasDosRutas() throws Exception {
        String token = token(999_998L, "PERSONA_MAYOR");
        saludo(token).andExpect(status().isNotFound());
        enviar("{\"mensaje\":\"Hola\"}", token).andExpect(status().isNotFound());
        assertTrue(modelo.instrucciones.isEmpty());
    }

    // ---------- Saludo ----------

    @Test
    void elSaludoCambiaConLaFranjaHorariaYLlevaElNombre() throws Exception {
        String[][] casos = {
                { "05:59", "Buenas noches" }, { "06:00", "Buen día" }, { "11:59", "Buen día" },
                { "12:00", "Buenas tardes" }, { "19:59", "Buenas tardes" }, { "20:00", "Buenas noches" } };
        for (String[] caso : casos) {
            reloj.fijar(HOY.atTime(java.time.LocalTime.parse(caso[0])));
            saludo(tokenA).andExpect(status().isOk())
                    .andExpect(jsonPath("$.saludo").value(caso[1] + ", " + NOMBRE_A));
        }
    }

    @Test
    void elMensajeDelSaludoDependeDeLosRecordatoriosDeHoy() throws Exception {
        saludo(tokenA).andExpect(jsonPath("$.mensaje").value("Qué lindo verte."));

        crear(tokenA, "/citas", cita(HOY + "T16:30"));
        saludo(tokenA).andExpect(jsonPath("$.mensaje").value("Qué lindo verte. Hoy tenés un recordatorio."));

        crear(tokenA, "/medicacion", medicacion("Enalapril", "18:00"));
        saludo(tokenA).andExpect(jsonPath("$.mensaje").value("Qué lindo verte. Hoy tenés recordatorios."));

        // Los de otra persona no cuentan.
        saludo(tokenB).andExpect(jsonPath("$.mensaje").value("Qué lindo verte."));
    }

    // ---------- Mensaje: recorrido ----------

    @Test
    void mensajeNormalDevuelveLaRespuestaDelModeloYLeLlegaElPromptCompleto() throws Exception {
        enviar("{\"mensaje\":\"Hoy desayuné mate con tortas fritas\"}", tokenA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Respuesta del modelo falso."));

        assertEquals(1, modelo.instrucciones.size());
        String prompt = modelo.ultimasInstrucciones();
        assertTrue(prompt.startsWith("Sos Alma, una asistente digital"));
        assertFalse(MARCADOR_SIN_REEMPLAZAR.matcher(prompt).find(), "Quedó un marcador sin reemplazar");
        assertTrue(prompt.contains(URGENCIA));
        assertTrue(prompt.contains(MALESTAR));
        assertTrue(prompt.contains("Historia de vida: No hay información."));
        assertTrue(prompt.contains("Recuerdos relacionados: No hay información."));

        List<TurnoConversacion> turnos = modelo.ultimosTurnos();
        assertEquals(1, turnos.size());
        assertEquals(TurnoConversacion.Rol.PERSONA, turnos.get(0).rol());
        assertEquals("Hoy desayuné mate con tortas fritas", turnos.get(0).texto());
    }

    @Test
    void elMensajeNuevoVaAlFinalDespuesDeLaConversacionConSusRoles() throws Exception {
        enviar("""
                {"mensaje":"Ver a mi nieta","conversacion":[
                  {"rol":"ALMA","texto":"¿Qué fue lo mejor de tu día?"},
                  {"rol":"PERSONA","texto":"Hablar con mi hijo"}]}""", tokenA).andExpect(status().isOk());

        List<TurnoConversacion> turnos = modelo.ultimosTurnos();
        assertEquals(3, turnos.size());
        assertEquals(new TurnoConversacion(TurnoConversacion.Rol.ALMA, "¿Qué fue lo mejor de tu día?"), turnos.get(0));
        assertEquals(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Hablar con mi hijo"), turnos.get(1));
        assertEquals(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, "Ver a mi nieta"), turnos.get(2));
    }

    @Test
    void urgenciaMedicaDevuelveElTextoPredefinidoSinLlamarAlModelo() throws Exception {
        enviar("{\"mensaje\":\"Tengo un dolor fuerte en el pecho y me falta el aire\"}", tokenA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(URGENCIA));
        enviar("{\"mensaje\":\"Me tomé dos pastillas de más, ¿pasa algo?\"}", tokenA)
                .andExpect(jsonPath("$.respuesta").value(URGENCIA));

        assertTrue(modelo.instrucciones.isEmpty(), "El modelo no debía ser llamado");
    }

    @Test
    void malestarGraveDevuelveElTextoPredefinidoSinLlamarAlModelo() throws Exception {
        enviar("{\"mensaje\":\"Ya no quiero vivir\"}", tokenA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value(MALESTAR));
        // Si coinciden las dos listas, gana malestar grave.
        enviar("{\"mensaje\":\"Me duele el pecho y quiero morirme\"}", tokenA)
                .andExpect(jsonPath("$.respuesta").value(MALESTAR));

        assertTrue(modelo.instrucciones.isEmpty(), "El modelo no debía ser llamado");
    }

    @Test
    void laConversacionAnteriorNoSeEvaluaConLasListas() throws Exception {
        enviar("""
                {"mensaje":"Sí, gracias","conversacion":[{"rol":"PERSONA","texto":"Me duele el pecho"}]}""", tokenA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Respuesta del modelo falso."));
        assertEquals(1, modelo.instrucciones.size());
    }

    // ---------- Mensaje: contexto ----------

    @Test
    void elPromptNoLlevaElNombreElApellidoNiElUsuario() throws Exception {
        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());

        String prompt = modelo.ultimasInstrucciones();
        assertFalse(prompt.contains(NOMBRE_A));
        assertFalse(prompt.contains(APELLIDO_A));
        assertFalse(prompt.contains(USUARIO_A));
    }

    @Test
    void animoSinRegistroYConRegistroYSinRecordatorios() throws Exception {
        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());
        String sin = modelo.ultimasInstrucciones();
        assertTrue(sin.contains("Ánimo de hoy: No registró cómo se siente hoy."));
        assertTrue(sin.contains("Recordatorios de hoy: No tiene recordatorios para hoy."));

        String[][] niveles = { { "MUY_MAL", "Muy mal" }, { "MAL", "Mal" }, { "NEUTRO", "Más o menos" },
                { "BIEN", "Bien" }, { "MUY_BIEN", "Muy bien" } };
        for (String[] nivel : niveles) {
            registrarAnimo(tokenA, nivel[0]);
            enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());
            assertTrue(modelo.ultimasInstrucciones().contains("Ánimo de hoy: " + nivel[1] + "\n"), nivel[1]);
        }
    }

    @Test
    void losRecordatoriosDeHoyVanUnoPorLineaEnOrdenDeHoraSinMedicoNiLugar() throws Exception {
        crear(tokenA, "/medicacion", medicacion("Enalapril", "18:00"));
        crear(tokenA, "/citas", cita(HOY + "T16:30"));

        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());

        String prompt = modelo.ultimasInstrucciones();
        assertTrue(prompt.contains("""
                Recordatorios de hoy: - 16:30, cita médica: Control anual, Cardiología (pendiente)
                - 18:00, medicación: Enalapril, 1 pastilla (pendiente)
                Historia de vida:"""), prompt);
        assertFalse(prompt.contains("Dra. Pérez"));
        assertFalse(prompt.contains("Consultorio Centro"));
        assertFalse(prompt.contains("Llevar los estudios"));
    }

    @Test
    void elEstadoDeLosRecordatoriosSeInformaConLosValoresReales() throws Exception {
        crear(tokenA, "/medicacion", medicacion("Enalapril", "18:00"));
        crear(tokenA, "/citas", cita(HOY + "T16:30"));

        // Pasó la hora de la medicación y sigue pendiente.
        reloj.fijar(HOY.atTime(19, 0));
        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());
        assertTrue(modelo.ultimasInstrucciones().contains("medicación: Enalapril, 1 pastilla (pendiente, ya pasó la hora)"));

        // La toma y la cita, atendidas.
        Long medicacion = notificacionDe("medicación", tokenA);
        Long cita = notificacionDe("cita", tokenA);
        mockMvc.perform(put("/api/recordatorios/notificaciones/" + medicacion + "/estado")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"TOMADA\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/recordatorios/notificaciones/" + cita + "/estado")
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenA)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"VISTA\"}")).andExpect(status().isOk());

        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());
        String prompt = modelo.ultimasInstrucciones();
        assertTrue(prompt.contains("medicación: Enalapril, 1 pastilla (ya tomada)"), prompt);
        assertTrue(prompt.contains("cita médica: Control anual, Cardiología (ya vista)"), prompt);
    }

    @Test
    void unTituloConUnMarcadorNoSeReemplaza() throws Exception {
        registrarAnimo(tokenA, "BIEN");
        crear(tokenA, "/citas", cita(HOY + "T16:30").replace("Control anual", "Ver {animo} y {progreso}"));

        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());

        assertTrue(modelo.ultimasInstrucciones().contains("cita médica: Ver {animo} y {progreso}, Cardiología"));
    }

    @Test
    void losDatosDeOtraPersonaNuncaAparecenEnElPrompt() throws Exception {
        crear(tokenB, "/medicacion", medicacion("Losartan", "18:00"));
        registrarAnimo(tokenB, "MUY_MAL");

        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());

        String prompt = modelo.ultimasInstrucciones();
        assertFalse(prompt.contains("Losartan"));
        assertTrue(prompt.contains("Ánimo de hoy: No registró cómo se siente hoy."));
        assertTrue(prompt.contains("Recordatorios de hoy: No tiene recordatorios para hoy."));
    }

    // ---------- Mensaje: conversación ----------

    @Test
    void conMasDeDiezTurnosElModeloRecibeSoloLosUltimosDiezMasElMensajeNuevo() throws Exception {
        StringBuilder turnos = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            turnos.append(i > 1 ? "," : "").append("{\"rol\":\"").append(i % 2 == 0 ? "ALMA" : "PERSONA")
                    .append("\",\"texto\":\"turno ").append(i).append("\"}");
        }
        enviar("{\"mensaje\":\"nuevo\",\"conversacion\":[" + turnos + "]}", tokenA).andExpect(status().isOk());

        List<TurnoConversacion> recibidos = modelo.ultimosTurnos();
        assertEquals(11, recibidos.size());
        assertEquals("turno 3", recibidos.get(0).texto());
        assertEquals("turno 12", recibidos.get(9).texto());
        assertEquals("nuevo", recibidos.get(10).texto());
    }

    @Test
    void unTextoDeLaConversacionDeMasDe2000CaracteresSeCortaSinError() throws Exception {
        String largo = "a".repeat(2500);
        enviar("{\"mensaje\":\"Hola\",\"conversacion\":[{\"rol\":\"ALMA\",\"texto\":\"" + largo + "\"}]}", tokenA)
                .andExpect(status().isOk());

        assertEquals(2000, modelo.ultimosTurnos().get(0).texto().length());
    }

    @Test
    void sinConversacionOConConversacionVaciaSeMandaSoloElMensaje() throws Exception {
        enviar("{\"mensaje\":\"Hola\"}", tokenA).andExpect(status().isOk());
        assertEquals(1, modelo.ultimosTurnos().size());
        enviar("{\"mensaje\":\"Hola\",\"conversacion\":[]}", tokenA).andExpect(status().isOk());
        assertEquals(1, modelo.ultimosTurnos().size());
    }

    // ---------- Mensaje: validaciones ----------

    @Test
    void mensajeVacioOSoloConEspaciosDevuelve400ConElTextoDelContrato() throws Exception {
        for (String json : List.of("{\"mensaje\":\"\"}", "{\"mensaje\":\"   \"}", "{}")) {
            enviar(json, tokenA)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                    .andExpect(jsonPath("$.errores[0].campo").value("mensaje"))
                    .andExpect(jsonPath("$.errores[0].mensaje").value("Escribí un mensaje para Alma."));
        }
        assertTrue(modelo.instrucciones.isEmpty());
    }

    @Test
    void mensajeDeMasDe500CaracteresDevuelve400YDe500Pasa() throws Exception {
        enviar("{\"mensaje\":\"" + "a".repeat(501) + "\"}", tokenA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("mensaje"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("Tu mensaje es muy largo. Probá con uno más corto."));
        assertTrue(modelo.instrucciones.isEmpty());

        enviar("{\"mensaje\":\"" + "a".repeat(500) + "\"}", tokenA).andExpect(status().isOk());
    }

    @Test
    void rolInvalidoOCuerpoIlegibleDevuelve400() throws Exception {
        for (String json : List.of(
                "{\"mensaje\":\"Hola\",\"conversacion\":[{\"rol\":\"ROBOT\",\"texto\":\"x\"}]}",
                "{\"mensaje\":\"Hola\",\"conversacion\":[{\"texto\":\"x\"}]}",
                "{\"mensaje\":\"Hola\",\"conversacion\":[{\"rol\":\"ALMA\"}]}",
                "{\"mensaje\":",
                "")) {
            enviar(json, tokenA)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."));
        }
        assertTrue(modelo.instrucciones.isEmpty());
    }

    // ---------- Mensaje: respuesta y fallas del modelo ----------

    @Test
    void conUnBloqueDeRazonamientoSeRespondeSoloElTextoFinal() throws Exception {
        modelo.respuesta = "<think>La persona saluda, debo ser cálida.</think>\n\n  Hola, ¿cómo estás?  ";
        enviar("{\"mensaje\":\"Hola\"}", tokenA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Hola, ¿cómo estás?"));
    }

    @Test
    void errorDelModeloRespuestaVaciaOSoloRazonamientoDevuelven503() throws Exception {
        modelo.error = new ModeloNoDisponibleException("detalle técnico: connection refused 10.0.0.1");
        verificar503(enviar("{\"mensaje\":\"Hola\"}", tokenA));

        // Un error inesperado de otro tipo también es 503 solo si el modelo lo convierte; aquí, demora simulada.
        modelo.error = new ModeloNoDisponibleException("tiempo de espera agotado a los 30000 ms");
        verificar503(enviar("{\"mensaje\":\"Hola\"}", tokenA));

        modelo.error = null;
        modelo.respuesta = "   ";
        verificar503(enviar("{\"mensaje\":\"Hola\"}", tokenA));

        modelo.respuesta = "<think>solo pensamiento</think>";
        verificar503(enviar("{\"mensaje\":\"Hola\"}", tokenA));
    }

    private void verificar503(ResultActions resultado) throws Exception {
        String cuerpo = resultado
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                // "type" vale about:blank y Spring lo omite del JSON, igual que en el resto de los errores de la API.
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value("Alma no disponible"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail")
                        .value("Alma no pudo responder en este momento. Intentá nuevamente en unos segundos."))
                .andExpect(jsonPath("$.instance").value("/api/chat/mensajes"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(cuerpo.contains("connection refused"));
        assertFalse(cuerpo.contains("30000"));
        assertFalse(cuerpo.contains("pensamiento"));
    }

    // ---------- Privacidad ----------

    @Test
    void ningunLogContieneElTextoDeLosMensajes(CapturedOutput salida) throws Exception {
        String secreto = "FRASE_SECRETA_XYZ";
        modelo.respuesta = "Respuesta con RESPUESTA_SECRETA_XYZ";
        enviar("{\"mensaje\":\"Hola " + secreto + "\"}", tokenA).andExpect(status().isOk());
        enviar("{\"mensaje\":\"Me duele el pecho " + secreto + "\"}", tokenA).andExpect(status().isOk());
        enviar("{\"mensaje\":\"Ya no quiero vivir " + secreto + "\"}", tokenA).andExpect(status().isOk());
        enviar("{\"mensaje\":\"" + secreto + "\",\"conversacion\":[{\"rol\":\"ALMA\",\"texto\":\"" + secreto + "\"}]}",
                tokenA).andExpect(status().isOk());
        modelo.error = new ModeloNoDisponibleException("falla");
        enviar("{\"mensaje\":\"Hola " + secreto + "\"}", tokenA).andExpect(status().isServiceUnavailable());
        enviar("{\"mensaje\":\"" + secreto + " " + "a".repeat(600) + "\"}", tokenA).andExpect(status().isBadRequest());

        assertFalse(salida.getAll().contains(secreto), "El log contiene el texto de un mensaje");
        assertFalse(salida.getAll().contains("RESPUESTA_SECRETA_XYZ"), "El log contiene una respuesta");
        assertFalse(salida.getAll().contains("Sos Alma"), "El log contiene el prompt");
    }

    // ---------- Ayudas ----------

    private ResultActions saludo(String token) throws Exception {
        return mockMvc.perform(get("/api/chat/saludo").header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions enviar(String json, String token) throws Exception {
        return mockMvc.perform(post("/api/chat/mensajes").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void crear(String token, String ruta, String json) throws Exception {
        mockMvc.perform(post("/api/recordatorios" + ruta).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isCreated());
    }

    private void registrarAnimo(String token, String nivel) throws Exception {
        mockMvc.perform(post("/api/animo").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nivelAnimo\":\"" + nivel + "\"}"))
                .andExpect(status().isCreated());
    }

    /** Id de la notificación de hoy de ese tipo ("medicación" o "cita"), leída de la agenda. */
    private Long notificacionDe(String tipo, String token) throws Exception {
        String agenda = mockMvc.perform(get("/api/recordatorios/agenda?desde=" + HOY + "&hasta=" + HOY)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String tipoApi = tipo.equals("cita") ? "CITA" : "MEDICACION";
        List<Integer> ids = JsonPath.read(agenda, "$[?(@.recordatorio.tipo == '" + tipoApi + "')].notificacionId");
        return ids.get(0).longValue();
    }

    private static String medicacion(String nombre, String hora) {
        return """
                {"nombreMedicamento":"%s","dosis":"1 pastilla","horaToma":"%s","frecuencia":"DIARIA",\
                "duracionDias":1}""".formatted(nombre, hora);
    }

    private static String cita(String fechaHora) {
        return """
                {"titulo":"Control anual","especialidad":"Cardiología","medico":"Dra. Pérez",\
                "lugar":"Consultorio Centro","fechaHora":"%s","indicacionesPrevias":"Llevar los estudios."}"""
                .formatted(fechaHora);
    }

    private Long cuentaDePrueba(String nombreUsuario, String nombre, String apellido) {
        return cuentaRepository.findByNombreUsuario(nombreUsuario).map(Cuenta::getId).orElseGet(() -> {
            Cuenta cuenta = new Cuenta();
            cuenta.setNombreUsuario(nombreUsuario);
            cuenta.setContrasenia(passwordEncoder.encode("Alma2026!"));
            cuenta.setRol(Rol.PERSONA_MAYOR);
            cuenta.setFechaAlta(LocalDate.now());
            cuenta = cuentaRepository.save(cuenta);

            PreferenciaAccesibilidad preferencia = new PreferenciaAccesibilidad();
            preferencia.setTamanioLetra(TamanioLetra.NORMAL);
            preferencia.setAltoContraste(false);
            PersonaMayor persona = new PersonaMayor();
            persona.setNombre(nombre);
            persona.setApellido(apellido);
            persona.setCuenta(cuenta);
            persona.setPreferenciaAccesibilidad(preferencia);
            personaMayorRepository.save(persona);
            return cuenta.getId();
        });
    }

    private String token(Long cuentaId, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("alma")
                .subject(String.valueOf(cuentaId))
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
