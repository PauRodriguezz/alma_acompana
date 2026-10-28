package ar.edu.utn.frm.gelab.alma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Login con los usuarios del seed (DatosDePrueba), igual que el helper de AuthIntegrationTests.
 */
public final class SesionDePrueba {

    public static final String CONTRASENIA = "Alma2026!";

    private SesionDePrueba() {
    }

    public static String token(MockMvc mockMvc, String nombreUsuario) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"" + nombreUsuario + "\",\"contrasenia\":\"" + CONTRASENIA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
