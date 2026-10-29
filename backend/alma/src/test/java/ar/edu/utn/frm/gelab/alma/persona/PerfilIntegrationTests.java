package ar.edu.utn.frm.gelab.alma.persona;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Pruebas de GET /api/perfil (contrato de login, sección 4.4). Usan el Postgres de Docker y los
 * usuarios del seed (ana y admin). Las cuentas temporales llevan el prefijo test_perfil_ y se
 * borran antes y después de cada prueba (también si la prueba falla).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilIntegrationTests {

    private static final String PERFIL = "/api/perfil";
    private static final String CONTRASENIA = "Alma2026!";
    private static final String SIN_FECHA = "test_perfil_sin_fecha";
    private static final String SIN_PERSONA = "test_perfil_sin_persona";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void borrarCuentasTemporales() {
        for (String nombreUsuario : List.of(SIN_FECHA, SIN_PERSONA)) {
            cuentaRepository.findByNombreUsuario(nombreUsuario).ifPresent(cuenta -> {
                personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(personaMayorRepository::delete);
                cuentaRepository.delete(cuenta);
            });
        }
    }

    @Test
    void personaMayorVeSusCuatroDatos() throws Exception {
        mockMvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, bearer(tokenDeLogin("ana"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Pérez"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1950-05-12"))
                .andExpect(jsonPath("$.nombreUsuario").value("ana"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.rol").doesNotExist())
                .andExpect(jsonPath("$.contrasenia").doesNotExist());
    }

    @Test
    void sinFechaDeNacimientoDevuelveNull() throws Exception {
        Cuenta cuenta = crearCuenta(SIN_FECHA);
        crearPersona(cuenta, "Sin", "Fecha");

        mockMvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, bearer(tokenDe(cuenta.getId(), "PERSONA_MAYOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sin"))
                .andExpect(jsonPath("$.fechaNacimiento", nullValue()))
                .andExpect(jsonPath("$.nombreUsuario").value(SIN_FECHA));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(PERFIL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
    }

    @Test
    void administradorDevuelve403() throws Exception {
        mockMvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, bearer(tokenDeLogin("admin"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
    }

    @Test
    void cuentaSinPersonaMayorDevuelve404() throws Exception {
        Cuenta cuenta = crearCuenta(SIN_PERSONA);

        mockMvc.perform(get(PERFIL).header(HttpHeaders.AUTHORIZATION, bearer(tokenDe(cuenta.getId(), "PERSONA_MAYOR"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos lo que buscás."));
    }

    private Cuenta crearCuenta(String nombreUsuario) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNombreUsuario(nombreUsuario);
        cuenta.setContrasenia(passwordEncoder.encode(CONTRASENIA));
        cuenta.setRol(Rol.PERSONA_MAYOR);
        cuenta.setFechaAlta(LocalDate.now());
        return cuentaRepository.save(cuenta);
    }

    private void crearPersona(Cuenta cuenta, String nombre, String apellido) {
        PreferenciaAccesibilidad preferencia = new PreferenciaAccesibilidad();
        preferencia.setTamanioLetra(TamanioLetra.NORMAL);
        preferencia.setAltoContraste(false);
        PersonaMayor persona = new PersonaMayor();
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setCuenta(cuenta);
        persona.setPreferenciaAccesibilidad(preferencia);
        personaMayorRepository.save(persona);
    }

    private String tokenDeLogin(String nombreUsuario) throws Exception {
        String respuesta = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasenia\":\"" + CONTRASENIA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String tokenDe(Long cuentaId, String rol) {
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
