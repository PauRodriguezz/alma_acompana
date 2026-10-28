package ar.edu.utn.frm.gelab.alma.common;

import java.net.URI;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.edu.utn.frm.gelab.alma.common.exception.CredencialesInvalidasException;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Convierte las excepciones en Problem Details (application/problem+json) con los textos del contrato.
 * Los 401 y 403 de los filtros de seguridad los escriben el entry point y el access denied handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex, HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, MensajesError.TITULO_NO_AUTORIZADO,
                MensajesError.CREDENCIALES_INVALIDAS, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail noAutenticado(AuthenticationException ex, HttpServletRequest request) {
        return problema(HttpStatus.UNAUTHORIZED, MensajesError.TITULO_NO_AUTORIZADO,
                MensajesError.SESION_TERMINADA, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail sinPermiso(AccessDeniedException ex, HttpServletRequest request) {
        return problema(HttpStatus.FORBIDDEN, MensajesError.TITULO_SIN_PERMISO,
                MensajesError.SIN_PERMISO, request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return problema(HttpStatus.NOT_FOUND, MensajesError.TITULO_NO_ENCONTRADO,
                MensajesError.NO_ENCONTRADO, request);
    }

    /** 400 de reglas validadas en un servicio: detail propio del caso de uso y la lista "errores". */
    @ExceptionHandler(DatosInvalidosException.class)
    public ProblemDetail datosInvalidos(DatosInvalidosException ex, HttpServletRequest request) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, MensajesError.TITULO_DATOS_INVALIDOS,
                ex.getMessage(), request);
        problema.setProperty("errores", ex.getErrores());
        return problema;
    }

    /** Cualquier otro error: se loguea completo, pero la persona nunca ve el detalle técnico. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInterno(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, MensajesError.TITULO_ERROR_INTERNO,
                MensajesError.ERROR_INTERNO, request);
    }

    /** 400 de Bean Validation: agrega la lista "errores" con cada campo y su mensaje. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problema = ex.getBody();
        problema.setProperty("errores", errores);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /** 413 de la subida de imágenes: Spring ya lo maneja, solo se cambia el texto. */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problema = ex.getBody();
        problema.setTitle(MensajesError.TITULO_IMAGEN_PESADA);
        problema.setDetail(MensajesError.IMAGEN_PESADA);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /** Pasa al español los errores que resuelve Spring MVC (JSON mal formado, ruta inexistente, etc.). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // Algunos casos (por ejemplo, ruta inexistente) llegan sin cuerpo: se usa el de la excepción.
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problema) {
            traducir(problema, statusCode);
            if (request instanceof ServletWebRequest servletRequest) {
                problema.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static void traducir(ProblemDetail problema, HttpStatusCode status) {
        switch (status.value()) {
            case 400 -> textos(problema, MensajesError.TITULO_DATOS_INVALIDOS, MensajesError.DATOS_INVALIDOS);
            case 401 -> textos(problema, MensajesError.TITULO_NO_AUTORIZADO, MensajesError.SESION_TERMINADA);
            case 403 -> textos(problema, MensajesError.TITULO_SIN_PERMISO, MensajesError.SIN_PERMISO);
            case 404 -> textos(problema, MensajesError.TITULO_NO_ENCONTRADO, MensajesError.NO_ENCONTRADO);
            case 500 -> textos(problema, MensajesError.TITULO_ERROR_INTERNO, MensajesError.ERROR_INTERNO);
            default -> {
                // Otros códigos (405, 415, ...) quedan con el texto de Spring.
            }
        }
    }

    private static void textos(ProblemDetail problema, String titulo, String detalle) {
        problema.setTitle(titulo);
        problema.setDetail(detalle);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle,
            HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }
}
