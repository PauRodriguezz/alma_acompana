package ar.edu.utn.frm.gelab.alma.historia.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.dto.EstadoGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.ProximaPreguntaResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RepreguntaSiguienteResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaTextoRequest;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.historia.service.RepreguntaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Historia de vida: guion, progreso y repreguntas (contrato del grafo, sección 7, endpoints 7.1 a 7.7). Solo
 * PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/historia")
@Tag(name = "Historia de vida")
public class HistoriaController {

    private final HistoriaDeVidaService historiaService;
    private final RepreguntaService repreguntaService;

    public HistoriaController(HistoriaDeVidaService historiaService, RepreguntaService repreguntaService) {
        this.historiaService = historiaService;
        this.repreguntaService = repreguntaService;
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
    @ApiResponse(responseCode = "202", description = "Respuesta registrada; el recuerdo se arma en segundo plano")
    public ResponseEntity<Void> responder(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer preguntaId,
            @Valid @RequestBody RespuestaTextoRequest datos) {
        historiaService.responder(cuentaId(jwt), preguntaId, datos.texto());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/guion/{preguntaId}/salteo")
    @Operation(summary = "Saltear una pregunta del guion",
            description = "Una pregunta ya respondida o salteada da 409; una que no existe, 404.")
    @ApiResponse(responseCode = "204", description = "Pregunta salteada")
    public ResponseEntity<Void> saltear(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer preguntaId) {
        historiaService.saltear(cuentaId(jwt), preguntaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/repreguntas/siguiente")
    @Operation(summary = "Ver si hay una repregunta",
            description = "PROCESANDO si hay una extracción en cola o en curso (volver a preguntar en unos "
                    + "segundos), HAY con la repregunta (recuerdo, dato y texto) o NINGUNA. Sin preguntaId busca en "
                    + "todos los recuerdos y devuelve el más antiguo con una candidata. excluir: datos separados por "
                    + "coma (ANIO, LUGARES, PERSONAS) que ya se preguntaron en esta charla. Un valor inválido da 400.")
    public RepreguntaSiguienteResponse siguienteRepregunta(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer preguntaId,
            @RequestParam(required = false) List<DatoFaltante> excluir) {
        return repreguntaService.siguiente(cuentaId(jwt), preguntaId, excluir);
    }

    @PostMapping("/repreguntas/{recuerdoId}/{dato}/respuesta")
    @Operation(summary = "Responder una repregunta",
            description = "Cuenta un intento y completa el recuerdo en segundo plano, solo con lo del dato "
                    + "preguntado. Un recuerdo que no existe o no es de la persona da 404; un dato que no aplica, "
                    + "que ya no falta o que ya se preguntó 2 veces, 409; una respuesta vacía o de más de 1000 "
                    + "caracteres, 400.")
    @ApiResponse(responseCode = "202", description = "Intento contado; el recuerdo se completa en segundo plano")
    public ResponseEntity<Void> responderRepregunta(@AuthenticationPrincipal Jwt jwt, @PathVariable String recuerdoId,
            @PathVariable DatoFaltante dato, @Valid @RequestBody RespuestaTextoRequest datos) {
        repreguntaService.responder(cuentaId(jwt), recuerdoId, dato, datos.texto());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/repreguntas/{recuerdoId}/{dato}/salteo")
    @Operation(summary = "Saltear una repregunta",
            description = "Cuenta un intento igual que responder y no cambia el recuerdo. Mismos errores que "
                    + "responder, salvo el del texto.")
    @ApiResponse(responseCode = "204", description = "Intento contado")
    public ResponseEntity<Void> saltearRepregunta(@AuthenticationPrincipal Jwt jwt, @PathVariable String recuerdoId,
            @PathVariable DatoFaltante dato) {
        repreguntaService.saltear(cuentaId(jwt), recuerdoId, dato);
        return ResponseEntity.noContent().build();
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
