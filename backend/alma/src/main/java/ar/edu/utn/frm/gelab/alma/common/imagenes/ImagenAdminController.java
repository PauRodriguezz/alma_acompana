package ar.edu.utn.frm.gelab.alma.common.imagenes;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Subida de imágenes del panel de operador (contrato del panel, sección 3.4). Solo ADMINISTRADOR.
 * Las imágenes se sirven después, públicas, en GET /imagenes/{nombre}.
 */
@RestController
@RequestMapping("/api/admin/imagenes")
@Tag(name = "Panel · Imágenes")
public class ImagenAdminController {

    private final AlmacenImagenes almacenImagenes;

    public ImagenAdminController(AlmacenImagenes almacenImagenes) {
        this.almacenImagenes = almacenImagenes;
    }

    /** Sin la parte "archivo" responde lo mismo que con un archivo vacío. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir una imagen JPG, PNG o WEBP de hasta 2 MB")
    public ImagenSubidaResponse subir(@RequestPart(name = "archivo", required = false) MultipartFile archivo) {
        return new ImagenSubidaResponse(almacenImagenes.guardar(archivo));
    }
}
