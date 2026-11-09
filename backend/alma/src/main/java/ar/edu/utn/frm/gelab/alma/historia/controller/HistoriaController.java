package ar.edu.utn.frm.gelab.alma.historia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.historia.dto.EstadoGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.ProximaPreguntaResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaTextoRequest;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Historia de vida: guion y progreso (contrato del grafo, sección 7, endpoints 7.1 a 7.4). Solo
 * PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/historia")
@Tag(name = "Historia de vida")
public class HistoriaController {

    private final HistoriaDeVidaService historiaService;

    public HistoriaController(HistoriaDeVidaService historiaService) {
        this.historiaService = historiaService;
    }

    @GetMapping("/guion/proxima")
    @Operation(summary = "Ver la próxima pregunta del guion",
            description = "La pregunta pendiente de menor orden, con una introducción fija. Si no queda "
                    + "ninguna, devuelve el mensaje final. Si la cuenta no tiene persona mayor, 404.")
    public ProximaPreguntaResponse proxima(@AuthenticationPrincipal Jwt jwt) {
        return historiaService.proxima(cuentaId(jwt));
    }

    @GetMapping("/guion/estado")
    @Operation(summary = "Ver el estado de todas las preguntas",
            description = "Cada pregunta con su estado (respondida, salteada o pendiente), los totales y la "
                    + "última respondida o salteada.")
    public EstadoGuionResponse estado(@AuthenticationPrincipal Jwt jwt) {
        return historiaService.estado(cuentaId(jwt));
    }

    @PostMapping("/guion/{preguntaId}/respuesta")
    @Operation(summary = "Responder una pregunta del guion",
            description = "Queda respondida de inmediato. Una pregunta ya respondida o salteada da 409; una "
                    + "que no existe, 404; una respuesta vacía o de más de 1000 caracteres, 400.")
    public ResponseEntity<Void> responder(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer preguntaId,
            @Valid @RequestBody RespuestaTextoRequest datos) {
        historiaService.responder(cuentaId(jwt), preguntaId, datos.texto());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/guion/{preguntaId}/salteo")
    @Operation(summary = "Saltear una pregunta del guion",
            description = "Una pregunta ya respondida o salteada da 409; una que no existe, 404.")
    public ResponseEntity<Void> saltear(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer preguntaId) {
        historiaService.saltear(cuentaId(jwt), preguntaId);
        return ResponseEntity.noContent().build();
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
