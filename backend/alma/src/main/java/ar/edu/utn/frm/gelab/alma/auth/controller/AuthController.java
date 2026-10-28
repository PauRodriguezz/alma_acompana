package ar.edu.utn.frm.gelab.alma.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.auth.dto.LoginRequest;
import ar.edu.utn.frm.gelab.alma.auth.dto.LoginResponse;
import ar.edu.utn.frm.gelab.alma.auth.dto.UsuarioResponse;
import ar.edu.utn.frm.gelab.alma.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Ingresar con nombre de usuario y contraseña")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** La cuenta sale siempre del token (claim sub), nunca de la URL ni del cuerpo. */
    @GetMapping("/me")
    @Operation(summary = "Ver el usuario de la sesión actual")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.usuarioActual(Long.valueOf(jwt.getSubject()));
    }
}
