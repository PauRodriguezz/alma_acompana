package ar.edu.utn.frm.gelab.alma.common.imagenes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;

/**
 * Guarda las imágenes que sube la persona operadora en la carpeta alma.uploads.dir.
 * El tipo se detecta por el contenido (firma del archivo), y el nombre lo genera el back.
 */
@Service
public class AlmacenImagenes {

    static final String TIPO_INVALIDO = "La imagen tiene que ser JPG, PNG o WEBP.";

    private static final byte[] FIRMA_JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};

    private final Path carpeta;

    public AlmacenImagenes(@Value("${alma.uploads.dir}") String carpeta) throws IOException {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
    }

    public Path getCarpeta() {
        return carpeta;
    }

    /** Devuelve la ruta pública de la imagen guardada ("/imagenes/<uuid>.<ext>"). */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw tipoInvalido();
        }
        try {
            String extension = extensionSegunContenido(archivo);
            String nombre = UUID.randomUUID() + "." + extension;
            Path destino = carpeta.resolve(nombre).normalize();
            if (!destino.getParent().equals(carpeta)) {
                throw tipoInvalido();
            }
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino);
            }
            return RutaImagen.PREFIJO + nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    private static String extensionSegunContenido(MultipartFile archivo) throws IOException {
        byte[] cabecera;
        try (InputStream entrada = archivo.getInputStream()) {
            cabecera = entrada.readNBytes(12);
        }
        if (empiezaCon(cabecera, 0, FIRMA_JPG)) {
            return "jpg";
        }
        if (empiezaCon(cabecera, 0, FIRMA_PNG)) {
            return "png";
        }
        if (empiezaCon(cabecera, 0, FIRMA_RIFF) && empiezaCon(cabecera, 8, FIRMA_WEBP)) {
            return "webp";
        }
        throw tipoInvalido();
    }

    private static boolean empiezaCon(byte[] datos, int desde, byte[] firma) {
        return datos.length >= desde + firma.length
                && Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
    }

    private static DatosInvalidosException tipoInvalido() {
        return new DatosInvalidosException(TIPO_INVALIDO, List.of(new ErrorCampo("archivo", TIPO_INVALIDO)));
    }
}
