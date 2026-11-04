package ar.edu.utn.frm.gelab.alma.bienestar.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.bienestar.dto.RegistrarAnimoRequest;
import ar.edu.utn.frm.gelab.alma.bienestar.dto.RegistroAnimoResponse;
import ar.edu.utn.frm.gelab.alma.bienestar.service.RegistroAnimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/animo")
@Tag(name = "Bienestar")
public class AnimoController {

    private final RegistroAnimoService animoService;

    public AnimoController(RegistroAnimoService animoService) {
        this.animoService = animoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar cómo me siento")
    public RegistroAnimoResponse registrar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RegistrarAnimoRequest datos) {
        return animoService.registrar(cuentaId(jwt), datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cambiar cómo me siento (corrige el ánimo de hoy)")
    public RegistroAnimoResponse modificar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody RegistrarAnimoRequest datos) {
        return animoService.modificar(cuentaId(jwt), id, datos);
    }

    @GetMapping("/hoy")
    @Operation(summary = "Ver el último ánimo que registré hoy (204 si todavía no registré)")
    public ResponseEntity<RegistroAnimoResponse> hoy(@AuthenticationPrincipal Jwt jwt) {
        return animoService.animoDeHoy(cuentaId(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
