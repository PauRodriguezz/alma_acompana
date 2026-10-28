package ar.edu.utn.frm.gelab.alma.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 403 cuando el rol del token no tiene permiso para la ruta.
 */
@Component
public class ProblemDetailsAccessDeniedHandler implements AccessDeniedHandler {

    private final EscritorProblemDetails escritor;

    public ProblemDetailsAccessDeniedHandler(EscritorProblemDetails escritor) {
        this.escritor = escritor;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escritor.escribir(request, response, HttpStatus.FORBIDDEN,
                MensajesError.TITULO_SIN_PERMISO, MensajesError.SIN_PERMISO);
    }
}
