package ar.edu.utn.frm.gelab.alma.common.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe un Problem Details desde los filtros de seguridad, donde no llega el @RestControllerAdvice.
 */
@Component
public class EscritorProblemDetails {

    private final JsonMapper jsonMapper;

    public EscritorProblemDetails(JsonMapper jsonMapper) {
        // El mismo mixin que usa el conversor HTTP de Spring, para que el JSON sea igual al del resto de la API.
        this.jsonMapper = jsonMapper.rebuild()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String titulo, String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), problema);
    }
}
