package ar.edu.utn.frm.gelab.alma.microcapsulas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaRequest;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaResumenResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.service.MicrocapsulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Microcápsulas en el panel de operador (CU-11). Solo ADMINISTRADOR (lo exige SecurityConfig en /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/microcapsulas")
@Tag(name = "Panel · Microcápsulas")
public class MicrocapsulaAdminController {

    private final MicrocapsulaService microcapsulaService;

    public MicrocapsulaAdminController(MicrocapsulaService microcapsulaService) {
        this.microcapsulaService = microcapsulaService;
    }

    @GetMapping
    @Operation(summary = "Listar las microcápsulas vigentes, de la más reciente a la más antigua")
    public List<MicrocapsulaResumenResponse> listar() {
        return microcapsulaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver el detalle de una microcápsula")
    public MicrocapsulaResponse obtener(@PathVariable Long id) {
        return microcapsulaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear y publicar una microcápsula")
    public MicrocapsulaResponse crear(@RequestBody MicrocapsulaRequest datos) {
        return microcapsulaService.crear(datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una microcápsula")
    public MicrocapsulaResponse actualizar(@PathVariable Long id, @RequestBody MicrocapsulaRequest datos) {
        return microcapsulaService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dar de baja una microcápsula (baja lógica)")
    public void darDeBaja(@PathVariable Long id) {
        microcapsulaService.darDeBaja(id);
    }
}
