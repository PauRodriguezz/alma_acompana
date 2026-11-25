package ar.edu.utn.frm.gelab.alma.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

/**
 * Lee los textos de las microcápsulas del seed desde {@code src/main/resources/microcapsulas/}. Cada archivo
 * tiene solo el texto (Markdown limitado), en UTF-8 y con saltos de línea LF.
 */
public final class TextosDeMicrocapsulas {

    private static final String CARPETA = "microcapsulas/";

    private TextosDeMicrocapsulas() {
    }

    /** Devuelve el texto del archivo, con saltos de línea {@code \n} y sin espacios ni saltos en los bordes. */
    public static String leer(String archivo) {
        try (InputStream entrada = new ClassPathResource(CARPETA + archivo).getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").strip();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el texto de la microcápsula: " + archivo, e);
        }
    }
}
