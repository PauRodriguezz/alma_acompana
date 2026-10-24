package ar.edu.utn.frm.gelab.alma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Pruebas del contrato de login (sección 11). Usan el Postgres de Docker y los usuarios
 * de prueba del seed: antes hay que correr "docker compose up -d".
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/auth/me";
    private static final String CONTRASENIA = "Alma2026!";
    private static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos. Intentá nuevamente.";
    private static final String SESION_TERMINADA = "Tu sesión terminó. Ingresá de nuevo.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void loginCorrectoDevuelveTokenYUsuario() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("ana", CONTRASENIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(30 * 24 * 60 * 60))
                .andExpect(jsonPath("$.usuario.nombreUsuario").value("ana"))
                .andExpect(jsonPath("$.usuario.nombre").value("Ana"))
                .andExpect(jsonPath("$.usuario.rol").value("PERSONA_MAYOR"))
                .andExpect(jsonPath("$.usuario.preferenciasAccesibilidad.tamanioLetra", notNullValue()));
    }

    @Test
    void loginSinMantenerSesionDuraUnDia() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"ana\",\"contrasenia\":\"" + CONTRASENIA
                                + "\",\"mantenerSesion\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEnSegundos").value(24 * 60 * 60));
    }

    @Test
    void contraseniaIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("ana", "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(CREDENCIALES_INVALIDAS))
                .andExpect(jsonPath("$.instance").value(LOGIN));
    }

    @Test
    void usuarioInexistenteDevuelveElMismo401() throws Exception {
        String contraseniaMala = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("ana", "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String usuarioInexistente = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("no-existe", "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(usuarioInexistente).isEqualTo(contraseniaMala);
    }

    @Test
    void loginSinCamposDevuelve400ConErrores() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores", hasSize(2)));
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(ME))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(SESION_TERMINADA));
    }

    @Test
    void meConTokenDevuelveElUsuario() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(tokenDe("ana"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("ana"))
                .andExpect(jsonPath("$.preferenciasAccesibilidad", notNullValue()));
    }

    @Test
    void meDelAdministradorNoTraePreferencias() throws Exception {
        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(tokenDe("admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.preferenciasAccesibilidad", nullValue()));
    }

    @Test
    void tokenVencidoDevuelve401() throws Exception {
        Instant hace2Dias = Instant.now().minusSeconds(2 * 24 * 60 * 60);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("alma")
                .subject("1")
                .claim("rol", "PERSONA_MAYOR")
                .issuedAt(hace2Dias)
                .expiresAt(hace2Dias.plusSeconds(60))
                .build();
        String vencido = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(vencido)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SESION_TERMINADA));
    }

    @Test
    void personaMayorEnRutaDeAdministradorDevuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/cualquiera").header(HttpHeaders.AUTHORIZATION, bearer(tokenDe("ana"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
    }

    @Test
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    private String tokenDe(String nombreUsuario) throws Exception {
        String respuesta = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(nombreUsuario, CONTRASENIA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private static String credenciales(String nombreUsuario, String contrasenia) {
        return "{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasenia\":\"" + contrasenia + "\"}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
