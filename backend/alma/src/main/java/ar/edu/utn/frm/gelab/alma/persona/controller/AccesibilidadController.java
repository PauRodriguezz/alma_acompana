package ar.edu.utn.frm.gelab.alma.persona.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.persona.dto.PreferenciaAccesibilidadDto;
import ar.edu.utn.frm.gelab.alma.persona.service.AccesibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/accesibilidad")
@Tag(name = "Accesibilidad")
public class AccesibilidadController {

    private final AccesibilidadService accesibilidadService;

    public AccesibilidadController(AccesibilidadService accesibilidadService) {
        this.accesibilidadService = accesibilidadService;
    }

    @GetMapping
    @Operation(summary = "Ver mis preferencias de accesibilidad")
    public PreferenciaAccesibilidadDto obtener(@AuthenticationPrincipal Jwt jwt) {
        return accesibilidadService.obtener(Long.valueOf(jwt.getSubject()));
    }

    @PutMapping
    @Operation(summary = "Guardar mis preferencias de accesibilidad")
    public PreferenciaAccesibilidadDto actualizar(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PreferenciaAccesibilidadDto datos) {
        return accesibilidadService.actualizar(Long.valueOf(jwt.getSubject()), datos);
    }
}
