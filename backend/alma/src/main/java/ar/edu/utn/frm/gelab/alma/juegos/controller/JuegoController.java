package ar.edu.utn.frm.gelab.alma.juegos.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import ar.edu.utn.frm.gelab.alma.juegos.MensajesJuego;
import ar.edu.utn.frm.gelab.alma.juegos.SinJuegosDisponiblesException;
import ar.edu.utn.frm.gelab.alma.juegos.dto.RespuestaRequest;
import ar.edu.utn.frm.gelab.alma.juegos.dto.ResultadoResponse;
import ar.edu.utn.frm.gelab.alma.juegos.dto.SiguienteResponse;
import ar.edu.utn.frm.gelab.alma.juegos.service.JuegoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Juego de estimulación (CU-05). Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale del token.
 */
@RestController
@RequestMapping("/api/juegos")
@Tag(name = "Juegos")
public class JuegoController {

    private final JuegoService juegoService;

    public JuegoController(JuegoService juegoService) {
        this.juegoService = juegoService;
    }

    @GetMapping("/siguiente")
    @Operation(summary = "Ver el próximo ejercicio para jugar",
            description = "Lo elige el sistema. Las opciones vienen mezcladas y sin la respuesta correcta. "
                    + "No guarda nada: si la persona sale sin responder, no queda registro.")
    public SiguienteResponse siguiente(@AuthenticationPrincipal Jwt jwt) {
        return juegoService.siguiente(cuentaId(jwt));
    }

    @PostMapping("/respuesta")
    @Operation(summary = "Responder un ejercicio",
            description = "En opción múltiple se manda opcionId; en ordenar, ordenOpcionIds con todos los pasos "
                    + "en el orden elegido. Devuelve si acertó, la respuesta correcta y la explicación.")
    public ResultadoResponse responder(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RespuestaRequest datos) {
        return juegoService.responder(cuentaId(jwt), datos);
    }

    /** 404 con texto propio: el handler global siempre responde "No encontramos lo que buscás.". */
    @ExceptionHandler(SinJuegosDisponiblesException.class)
    public ProblemDetail sinJuegos(SinJuegosDisponiblesException ex, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                MensajesJuego.SIN_JUEGOS_DISPONIBLES);
        problema.setTitle(MensajesError.TITULO_NO_ENCONTRADO);
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
