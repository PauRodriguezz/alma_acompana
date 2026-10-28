package ar.edu.utn.frm.gelab.alma.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 cuando falta el token, es inválido o venció.
 */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final EscritorProblemDetails escritor;

    public ProblemDetailsAuthenticationEntryPoint(EscritorProblemDetails escritor) {
        this.escritor = escritor;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escritor.escribir(request, response, HttpStatus.UNAUTHORIZED,
                MensajesError.TITULO_NO_AUTORIZADO, MensajesError.SESION_TERMINADA);
    }
}
