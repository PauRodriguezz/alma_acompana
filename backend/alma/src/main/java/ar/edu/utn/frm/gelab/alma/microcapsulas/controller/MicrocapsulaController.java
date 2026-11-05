package ar.edu.utn.frm.gelab.alma.microcapsulas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaLecturaDetalleResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaLecturaResumenResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.service.LecturaMicrocapsulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lectura de microcápsulas (CU-06). Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale del token.
 * El panel de operador sigue en /api/admin/microcapsulas (MicrocapsulaAdminController).
 */
@RestController
@RequestMapping("/api/microcapsulas")
@Tag(name = "Microcápsulas")
public class MicrocapsulaController {

    private final LecturaMicrocapsulaService lecturaService;

    public MicrocapsulaController(LecturaMicrocapsulaService lecturaService) {
        this.lecturaService = lecturaService;
    }

    @GetMapping
    @Operation(summary = "Ver las microcápsulas vigentes",
            description = "De la más nueva a la más vieja. Cada una dice si la persona la tiene guardada.")
    public List<MicrocapsulaLecturaResumenResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return lecturaService.listar(cuentaId(jwt));
    }

    /** Ruta fija: Spring la prefiere a "/{id}", así que no choca. */
    @GetMapping("/guardadas")
    @Operation(summary = "Ver las microcápsulas guardadas",
            description = "Las que la persona guardó, estén vigentes o no, de la última guardada a la primera.")
    public List<MicrocapsulaLecturaResumenResponse> guardadas(@AuthenticationPrincipal Jwt jwt) {
        return lecturaService.guardadas(cuentaId(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver una microcápsula",
            description = "Responde si está vigente o si la persona la tiene guardada. Si no, 404.")
    public MicrocapsulaLecturaDetalleResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return lecturaService.detalle(cuentaId(jwt), id);
    }

    @PutMapping("/{id}/guardada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Guardar una microcápsula",
            description = "Idempotente: si ya estaba guardada no hace nada. 404 si no es accesible para la persona.")
    public void guardar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        lecturaService.guardar(cuentaId(jwt), id);
    }

    @DeleteMapping("/{id}/guardada")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar una microcápsula de guardadas",
            description = "Idempotente: si no estaba guardada no hace nada. 404 solo si la microcápsula no existe.")
    public void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        lecturaService.quitar(cuentaId(jwt), id);
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
