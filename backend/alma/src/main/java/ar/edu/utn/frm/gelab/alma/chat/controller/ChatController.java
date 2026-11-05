package ar.edu.utn.frm.gelab.alma.chat.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import ar.edu.utn.frm.gelab.alma.chat.MensajesChat;
import ar.edu.utn.frm.gelab.alma.chat.dto.MensajeChatRequest;
import ar.edu.utn.frm.gelab.alma.chat.dto.RespuestaChatResponse;
import ar.edu.utn.frm.gelab.alma.chat.dto.SaludoResponse;
import ar.edu.utn.frm.gelab.alma.chat.service.ChatService;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Chat con Alma (CU-02). Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale del token.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/saludo")
    @Operation(summary = "Ver el saludo inicial de Alma",
            description = "Según la hora de Argentina y los recordatorios de hoy. Si la cuenta no tiene "
                    + "persona mayor, 404.")
    public SaludoResponse saludo(@AuthenticationPrincipal Jwt jwt) {
        return chatService.saludar(cuentaId(jwt));
    }

    @PostMapping("/mensajes")
    @Operation(summary = "Mandar un mensaje a Alma",
            description = "Devuelve la respuesta completa. Ante una urgencia médica o un malestar emocional "
                    + "grave responde un texto predefinido sin consultar al modelo. Si el modelo falla, 503. "
                    + "No se guarda la conversación.")
    public RespuestaChatResponse mensaje(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MensajeChatRequest datos) {
        return chatService.responder(cuentaId(jwt), datos);
    }

    @ExceptionHandler(ModeloNoDisponibleException.class)
    public ProblemDetail modeloNoDisponible(ModeloNoDisponibleException ex, HttpServletRequest request) {
        // Solo el tipo de error: el detalle técnico nunca llega a la persona (5.7) y no lleva su texto (5.8).
        log.warn("Alma no disponible en {} ({})", request.getRequestURI(), ex.getClass().getSimpleName());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                MensajesChat.ALMA_NO_DISPONIBLE);
        problema.setTitle(MensajesChat.TITULO_ALMA_NO_DISPONIBLE);
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
