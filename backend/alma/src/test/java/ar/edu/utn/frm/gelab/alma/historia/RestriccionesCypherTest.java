package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * La copia de {@code restricciones.cypher} que usa la aplicación ({@code src/main/resources/historia/}) tiene que
 * ser igual a la del modelo de grafo ({@code docs/Modelo_de_grafo/}), salvo el comentario que avisa que es una
 * copia (contrato del grafo, sección 10; modelo de grafo, "Reglas de integridad"). Si alguien cambia una y no la
 * otra, esta prueba falla.
 *
 * <p>La del modelo no viaja dentro de la aplicación: se busca en el repositorio, subiendo desde el directorio de
 * trabajo. Si no hay un repositorio con {@code docs/} (por ejemplo, las pruebas corren sobre una copia de solo el
 * back), la prueba se saltea: sin esa carpeta no puede haber dos copias que se desincronicen.
 */
class RestriccionesCypherTest {

    private static final String RECURSO = "historia/restricciones.cypher";
    private static final String RELATIVA_AL_REPO = "docs/Modelo_de_grafo/restricciones.cypher";
    /** La única diferencia permitida: la línea que avisa que el archivo de la aplicación es una copia. */
    private static final String COMENTARIO_DE_LA_COPIA = "// COPIA de ";

    @Test
    void laCopiaDeLaAplicacionEsIgualALaDelModeloSalvoElComentario() throws IOException {
        Optional<Path> delModelo = buscarSubiendo(Path.of("").toAbsolutePath());
        assumeTrue(delModelo.isPresent(), "No hay " + RELATIVA_AL_REPO + " arriba del directorio de trabajo");

        List<String> modelo = normalizar(Files.readString(delModelo.get(), StandardCharsets.UTF_8));
        List<String> copia = normalizar(leerRecurso());

        assertThat(copia)
                .as("%s (copia de la aplicación) tiene que ser igual a %s, salvo el comentario \"%s…\"",
                        RECURSO, RELATIVA_AL_REPO, COMENTARIO_DE_LA_COPIA.trim())
                .isEqualTo(modelo);
        assertThat(modelo).as("el archivo del modelo no puede estar vacío").isNotEmpty();
    }

    @Test
    void laCopiaDeLaAplicacionAvisaQueEsUnaCopia() throws IOException {
        assertThat(leerRecurso()).contains(COMENTARIO_DE_LA_COPIA + "docs/Modelo_de_grafo/restricciones.cypher");
    }

    private static Optional<Path> buscarSubiendo(Path desde) {
        for (Path carpeta = desde; carpeta != null; carpeta = carpeta.getParent()) {
            Path candidato = carpeta.resolve(RELATIVA_AL_REPO);
            if (Files.isRegularFile(candidato)) {
                return Optional.of(candidato);
            }
        }
        return Optional.empty();
    }

    private static String leerRecurso() throws IOException {
        try (InputStream entrada = new ClassPathResource(RECURSO).getInputStream()) {
            return StreamUtils.copyToString(entrada, StandardCharsets.UTF_8);
        }
    }

    /** Sin la línea del comentario de la copia, sin diferencias de fin de línea ni espacios al final, sin líneas vacías. */
    private static List<String> normalizar(String texto) {
        return texto.replace("\r\n", "\n").lines()
                .filter(linea -> !linea.startsWith(COMENTARIO_DE_LA_COPIA))
                .map(String::stripTrailing)
                .filter(linea -> !linea.isBlank())
                .toList();
    }
}
