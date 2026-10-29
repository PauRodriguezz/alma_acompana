package ar.edu.utn.frm.gelab.alma.recordatorios.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.EstadoNotificacionRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.ItemAgendaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionRequest;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.RecordatorioResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.TipoRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.service.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Solo PERSONA_MAYOR (lo exige SecurityConfig). La persona sale siempre del token.
 */
@RestController
@RequestMapping("/api/recordatorios")
@Tag(name = "Recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    public RecordatorioController(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    @GetMapping
    @Operation(summary = "Listar todos mis recordatorios, con filtro opcional por tipo")
    public List<RecordatorioResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TipoRecordatorio tipo) {
        return recordatorioService.listar(cuentaId(jwt), tipo);
    }

    @GetMapping("/agenda")
    @Operation(summary = "Ver mis avisos entre dos fechas (máximo 62 días)")
    public List<ItemAgendaResponse> agenda(@AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoRecordatorio tipo) {
        return recordatorioService.agenda(cuentaId(jwt), desde, hasta, tipo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver el detalle de un recordatorio")
    public RecordatorioResponse detalle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return recordatorioService.detalle(cuentaId(jwt), id);
    }

    @PostMapping("/medicacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un recordatorio de medicación")
    public MedicacionResponse crearMedicacion(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MedicacionRequest datos) {
        return recordatorioService.crearMedicacion(cuentaId(jwt), datos);
    }

    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un recordatorio de cita médica")
    public CitaResponse crearCita(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CitaRequest datos) {
        return recordatorioService.crearCita(cuentaId(jwt), datos);
    }

    @PutMapping("/medicacion/{id}")
    @Operation(summary = "Editar un recordatorio de medicación")
    public MedicacionResponse editarMedicacion(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody MedicacionRequest datos) {
        return recordatorioService.editarMedicacion(cuentaId(jwt), id, datos);
    }

    @PutMapping("/citas/{id}")
    @Operation(summary = "Editar un recordatorio de cita médica")
    public CitaResponse editarCita(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody CitaRequest datos) {
        return recordatorioService.editarCita(cuentaId(jwt), id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un recordatorio y todos sus avisos")
    public void eliminar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        recordatorioService.eliminar(cuentaId(jwt), id);
    }

    @PutMapping("/notificaciones/{id}/estado")
    @Operation(summary = "Marcar un aviso como tomado o visto, o volverlo a pendiente")
    public ItemAgendaResponse cambiarEstado(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody EstadoNotificacionRequest datos) {
        return recordatorioService.cambiarEstado(cuentaId(jwt), id, datos);
    }

    private static Long cuentaId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
