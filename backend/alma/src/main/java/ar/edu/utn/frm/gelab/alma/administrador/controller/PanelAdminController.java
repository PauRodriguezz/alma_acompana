package ar.edu.utn.frm.gelab.alma.administrador.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.administrador.dto.ResumenPanelResponse;
import ar.edu.utn.frm.gelab.alma.administrador.service.PanelAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Inicio del panel de operador. Solo ADMINISTRADOR (lo exige SecurityConfig en /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/panel")
@Tag(name = "Panel · Inicio")
public class PanelAdminController {

    private final PanelAdminService panelAdminService;

    public PanelAdminController(PanelAdminService panelAdminService) {
        this.panelAdminService = panelAdminService;
    }

    @GetMapping("/resumen")
    @Operation(summary = "Contar los ejercicios activos y las microcápsulas vigentes")
    public ResumenPanelResponse resumen() {
        return panelAdminService.resumen();
    }
}
