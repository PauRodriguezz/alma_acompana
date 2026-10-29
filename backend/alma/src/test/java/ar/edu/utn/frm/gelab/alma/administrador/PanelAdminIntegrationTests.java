package ar.edu.utn.frm.gelab.alma.administrador;

import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.bearer;
import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Resumen del panel y acceso a /api/admin/**. Usan el Postgres de Docker ("docker compose up -d").
 * Cada test corre en una transacción que se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PanelAdminIntegrationTests {

    private static final String RESUMEN = "/api/admin/panel/resumen";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resumenCambiaAlCrearYAlDarDeBaja() throws Exception {
        String admin = bearer(token(mockMvc, "admin"));
        long[] inicial = resumen(admin);

        String microcapsula = mockMvc.perform(post("/api/admin/microcapsulas").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Para el resumen\",\"texto\":\"Texto\",\"categoria\":\"ALIMENTACION\","
                                + "\"fechaPublicacion\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String ejercicio = mockMvc.perform(post("/api/admin/ejercicios").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"SERIE\",\"formato\":\"OPCION_MULTIPLE\",\"consigna\":\"1, 2, 3…\","
                                + "\"nivelDificultad\":\"BASICO\",\"opciones\":[{\"texto\":\"4\",\"esCorrecta\":true},"
                                + "{\"texto\":\"5\",\"esCorrecta\":false},{\"texto\":\"6\",\"esCorrecta\":false}]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(resumen(admin)).containsExactly(inicial[0] + 1, inicial[1] + 1);

        mockMvc.perform(delete("/api/admin/ejercicios/" + JsonPath.read(ejercicio, "$.id"))
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/microcapsulas/" + JsonPath.read(microcapsula, "$.id"))
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNoContent());

        assertThat(resumen(admin)).containsExactly(inicial[0], inicial[1]);
    }

    @Test
    void personaMayorRecibe403EnTodoElPanel() throws Exception {
        String ana = bearer(token(mockMvc, "ana"));
        for (AbstractMockHttpServletRequestBuilder<?> pedido : rutasDelPanel()) {
            mockMvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, ana))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("No tenés permiso para ver esta pantalla."));
        }
    }

    @Test
    void sinTokenDevuelve401EnTodoElPanel() throws Exception {
        for (AbstractMockHttpServletRequestBuilder<?> pedido : rutasDelPanel()) {
            mockMvc.perform(pedido)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Tu sesión terminó. Ingresá de nuevo."));
        }
    }

    private long[] resumen(String admin) throws Exception {
        String cuerpo = mockMvc.perform(get(RESUMEN).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new long[] {
                ((Number) JsonPath.read(cuerpo, "$.ejerciciosActivos")).longValue(),
                ((Number) JsonPath.read(cuerpo, "$.microcapsulasVigentes")).longValue()};
    }

    private static List<AbstractMockHttpServletRequestBuilder<?>> rutasDelPanel() {
        return List.of(
                get(RESUMEN),
                get("/api/admin/ejercicios"),
                post("/api/admin/ejercicios").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/admin/ejercicios/1"),
                delete("/api/admin/ejercicios/1"),
                get("/api/admin/microcapsulas"),
                post("/api/admin/microcapsulas").contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/api/admin/microcapsulas/1"),
                multipart("/api/admin/imagenes")
                        .file(new MockMultipartFile("archivo", "a.png", "image/png", new byte[] {1, 2, 3})));
    }
}
