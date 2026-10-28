package ar.edu.utn.frm.gelab.alma.ejercicios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioRequest;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioResponse;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioResumenResponse;
import ar.edu.utn.frm.gelab.alma.ejercicios.service.EjercicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Catálogo de ejercicios en el panel de operador (CU-10). Solo ADMINISTRADOR (lo exige SecurityConfig en /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/ejercicios")
@Tag(name = "Panel · Ejercicios")
public class EjercicioAdminController {

    private final EjercicioService ejercicioService;

    public EjercicioAdminController(EjercicioService ejercicioService) {
        this.ejercicioService = ejercicioService;
    }

    @GetMapping
    @Operation(summary = "Listar los ejercicios activos, del más nuevo al más viejo (filtro opcional por categoría)")
    public List<EjercicioResumenResponse> listar(@RequestParam(required = false) CategoriaEjercicio categoria) {
        return ejercicioService.listar(categoria);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver el detalle de un ejercicio, con sus opciones")
    public EjercicioResponse obtener(@PathVariable Long id) {
        return ejercicioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un ejercicio")
    public EjercicioResponse crear(@RequestBody EjercicioRequest datos) {
        return ejercicioService.crear(datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un ejercicio (reemplaza todas sus opciones)")
    public EjercicioResponse actualizar(@PathVariable Long id, @RequestBody EjercicioRequest datos) {
        return ejercicioService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un ejercicio del catálogo (baja lógica)")
    public void darDeBaja(@PathVariable Long id) {
        ejercicioService.darDeBaja(id);
    }
}
