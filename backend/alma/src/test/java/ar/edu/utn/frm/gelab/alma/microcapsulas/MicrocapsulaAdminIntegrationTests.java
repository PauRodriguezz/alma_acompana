package ar.edu.utn.frm.gelab.alma.microcapsulas;

import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.bearer;
import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.CategoriaMicrocapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaRequest;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * CU-11 desde el panel de operador (contrato del panel, secciones 3.3 y 4). Usan el Postgres de Docker
 * ("docker compose up -d"). Cada test corre en una transacción que se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MicrocapsulaAdminIntegrationTests {

    private static final String URL = "/api/admin/microcapsulas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String admin;

    @BeforeEach
    void ingresar() throws Exception {
        admin = token(mockMvc, "admin");
    }

    @Test
    void crearListarVerEditarYDarDeBaja() throws Exception {
        MicrocapsulaRequest nueva = new MicrocapsulaRequest("  Dormir mejor  ", "Acostate siempre a la misma hora.",
                CategoriaMicrocapsula.SUENIO_DESCANSO, null, Fechas.hoy());
        String creada = mockMvc.perform(conAdmin(post(URL)).content(json(nueva)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Dormir mejor"))
                .andExpect(jsonPath("$.fechaPublicacion").value(Fechas.hoy().toString()))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(creada, "$.id");

        mockMvc.perform(conAdmin(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].titulo").value("Dormir mejor"));

        mockMvc.perform(conAdmin(get(URL + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Acostate siempre a la misma hora."));

        MicrocapsulaRequest editada = new MicrocapsulaRequest("Dormir bien", "Texto nuevo.",
                CategoriaMicrocapsula.BIENESTAR_EMOCIONAL, "/imagenes/abc-123.png", LocalDate.of(2026, 10, 1));
        mockMvc.perform(conAdmin(put(URL + "/" + id)).content(json(editada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Dormir bien"))
                .andExpect(jsonPath("$.categoria").value("BIENESTAR_EMOCIONAL"))
                .andExpect(jsonPath("$.imagen").value("/imagenes/abc-123.png"));

        mockMvc.perform(conAdmin(delete(URL + "/" + id)))
                .andExpect(status().isNoContent());

        mockMvc.perform(conAdmin(get(URL)))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mockMvc.perform(conAdmin(get(URL + "/" + id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No encontramos lo que buscás."));
        mockMvc.perform(conAdmin(delete(URL + "/" + id)))
                .andExpect(status().isNotFound());

        // Baja lógica: la fila sigue, con la fecha de baja de hoy.
        entityManager.flush();
        LocalDate fechaBaja = jdbcTemplate.queryForObject(
                "select fecha_baja from microcapsula where id = ?", LocalDate.class, id);
        assertThat(fechaBaja).isEqualTo(Fechas.hoy());
    }

    @Test
    void camposVaciosDevuelve400ConErroresPorCampo() throws Exception {
        mockMvc.perform(conAdmin(post(URL)).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Completá todos los campos requeridos de la microcápsula."))
                .andExpect(jsonPath("$.errores", hasSize(4)))
                .andExpect(jsonPath("$.errores[?(@.campo == 'titulo')].mensaje").value("Ingresá el título."));
    }

    @Test
    void fechaFuturaDevuelve400() throws Exception {
        MicrocapsulaRequest futura = new MicrocapsulaRequest("Título", "Texto",
                CategoriaMicrocapsula.ALIMENTACION, null, Fechas.hoy().plusDays(1));
        mockMvc.perform(conAdmin(post(URL)).content(json(futura)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Revisá los datos ingresados."))
                .andExpect(jsonPath("$.errores[0].campo").value("fechaPublicacion"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("La fecha de publicación no puede ser futura."));
    }

    @Test
    void imagenQueNoEsDeLaSubidaDevuelve400() throws Exception {
        MicrocapsulaRequest conRutaMala = new MicrocapsulaRequest("Título", "Texto",
                CategoriaMicrocapsula.ALIMENTACION, "/imagenes/../application.properties", Fechas.hoy());
        mockMvc.perform(conAdmin(post(URL)).content(json(conRutaMala)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("imagen"));
    }

    private MockHttpServletRequestBuilder conAdmin(MockHttpServletRequestBuilder pedido) {
        return pedido.header(HttpHeaders.AUTHORIZATION, bearer(admin)).contentType(MediaType.APPLICATION_JSON);
    }

    private String json(Object cuerpo) {
        return jsonMapper.writeValueAsString(cuerpo);
    }
}
