package ar.edu.utn.frm.gelab.alma.persona.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.persona.dto.PerfilDto;
import ar.edu.utn.frm.gelab.alma.persona.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/perfil")
@Tag(name = "Perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    @Operation(summary = "Ver mis datos personales")
    public PerfilDto obtener(@AuthenticationPrincipal Jwt jwt) {
        return perfilService.obtener(Long.valueOf(jwt.getSubject()));
    }
}
