package ar.edu.utn.frm.gelab.alma;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Swagger y {@code /v3/api-docs}: con el perfil {@code prod} no existen (404) y sin ese perfil (desarrollo)
 * siguen abiertos. La salud queda abierta en los dos casos. Se prueba el arranque real, sin mocks de la
 * configuración.
 */
class PerfilProduccionTest {

    @Nested
    @SpringBootTest(properties = "spring.profiles.active=prod")
    @AutoConfigureMockMvc
    class ConPerfilProd {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void swaggerYApiDocsNoExisten() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
            mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        }

        @Test
        void laSaludSigueAbierta() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = "spring.profiles.active=desarrollo")
    @AutoConfigureMockMvc
    class SinPerfilProd {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void swaggerYApiDocsSiguenAbiertos() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
            // springdoc redirige /swagger-ui.html a /swagger-ui/index.html
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().is(either(is(200)).or(is(302))));
        }

        @Test
        void laSaludSigueAbierta() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
    }
}
