package ar.edu.utn.frm.gelab.alma.historia.controller;

import java.util.List;
import java.util.UUID;

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
import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoDeRespuesta;
import ar.edu.utn.frm.gelab.alma.historia.dto.EstadoGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.ListaDeRecuerdosResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.ProximaPreguntaResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RecuerdoDetalleResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RepreguntaSiguienteResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaDelGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaTextoRequest;
import ar.edu.utn.frm.gelab.alma.historia.service.ConsultaDeRecuerdos;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.historia.service.RepreguntaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final ConsultaDeRecuerdos consultaDeRecuerdos;

    public HistoriaController(HistoriaDeVidaService historiaService, RepreguntaService repreguntaService,
            ConsultaDeRecuerdos consultaDeRecuerdos) {
        this.historiaService = historiaService;
        this.repreguntaService = repreguntaService;
        this.consultaDeRecuerdos = consultaDeRecuerdos;
    }

    @GetMapping("/recuerdos")
    @Operation(summary = "Ver los recuerdos (línea de tiempo)",
            description = "Los recuerdos de la persona de más antiguo a más reciente por año; los que no tienen "
                    + "año van al final. Cada uno trae faltantes: los datos que se pueden seguir completando "
                    + "(ANIO, LUGARES, PERSONAS). Sin recuerdos, lista vacía.")
    public ListaDeRecuerdosResponse recuerdos(@AuthenticationPrincipal Jwt jwt) {
        return consultaDeRecuerdos.listar(cuentaId(jwt));
    }

    @GetMapping("/recuerdos/{recuerdoId}")
    @Operation(summary = "Ver el detalle de un recuerdo",
            description = "Título, descripción, año, personas, lugares, faltantes y el texto de la pregunta de la "
                    + "que surgió (preguntaTexto). Un recuerdo que no existe o es de otra persona da 404.")
    public RecuerdoDetalleResponse recuerdo(@AuthenticationPrincipal Jwt jwt, @PathVariable String recuerdoId) {
        return consultaDeRecuerdos.detalle(cuentaId(jwt), recuerdoId);
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
    @ApiResponse(responseCode = "202", description = "ACEPTADA: respuesta registrada; el recuerdo se arma en segundo plano")
    @ApiResponse(responseCode = "200", description = "ALARMA: el texto activó el detector de riesgo; se devuelve el "
            + "texto predefinido y no se guarda ni se encola nada (la pregunta sigue pendiente)")
    public ResponseEntity<RespuestaDelGuionResponse> responder(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer preguntaId, @Valid @RequestBody RespuestaTextoRequest datos) {
        return segunResultado(historiaService.responder(cuentaId(jwt), preguntaId, datos.texto()));
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
                    + "segundos), HAY con la repregunta (recuerdo, dato, texto y título del recuerdo) o NINGUNA. "
                    + "Sin preguntaId ni recuerdoId busca en todos los recuerdos y devuelve el más antiguo con una "
                    + "candidata. Con recuerdoId busca solo en ese recuerdo (uno inexistente o ajeno da NINGUNA). "
                    + "preguntaId y recuerdoId no se pueden mandar a la vez (400). excluir: datos separados por "
                    + "coma (ANIO, LUGARES, PERSONAS) que ya se preguntaron en esta charla. Un valor inválido o un "
                    + "recuerdoId mal formado da 400.")
    public RepreguntaSiguienteResponse siguienteRepregunta(@AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Solo los recuerdos que surgieron de esta pregunta del guion. Excluyente con "
                    + "recuerdoId.") @RequestParam(required = false) Integer preguntaId,
            @Parameter(description = "Solo este recuerdo (UUID). Excluyente con preguntaId.")
            @RequestParam(required = false) UUID recuerdoId,
            @RequestParam(required = false) List<DatoFaltante> excluir) {
        return repreguntaService.siguiente(cuentaId(jwt), preguntaId, recuerdoId, excluir);
    }

    @PostMapping("/repreguntas/{recuerdoId}/{dato}/respuesta")
    @Operation(summary = "Responder una repregunta",
            description = "Cuenta un intento y completa el recuerdo en segundo plano, solo con lo del dato "
                    + "preguntado. Un recuerdo que no existe o no es de la persona da 404; un dato que no aplica, "
                    + "que ya no falta o que ya se preguntó 2 veces, 409; una respuesta vacía o de más de 1000 "
                    + "caracteres, 400.")
    @ApiResponse(responseCode = "202", description = "ACEPTADA: intento contado; el recuerdo se completa en segundo plano")
    @ApiResponse(responseCode = "200", description = "ALARMA: el texto activó el detector de riesgo; se devuelve el "
            + "texto predefinido, no se cuenta el intento y no se encola nada")
    public ResponseEntity<RespuestaDelGuionResponse> responderRepregunta(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String recuerdoId, @PathVariable DatoFaltante dato,
            @Valid @RequestBody RespuestaTextoRequest datos) {
        return segunResultado(repreguntaService.responder(cuentaId(jwt), recuerdoId, dato, datos.texto()));
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

    /** 202 si se aceptó la respuesta; 200 si saltó la alarma (contrato de historia de vida, 7.1 y 7.2). */
    private static ResponseEntity<RespuestaDelGuionResponse> segunResultado(RespuestaDelGuionResponse resultado) {
        return resultado.estado() == EstadoDeRespuesta.ALARMA
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.accepted().body(resultado);
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
