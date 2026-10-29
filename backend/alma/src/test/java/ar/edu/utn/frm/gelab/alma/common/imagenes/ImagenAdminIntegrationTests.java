package ar.edu.utn.frm.gelab.alma.common.imagenes;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.jayway.jsonpath.JsonPath;

/**
 * Subida y descarga de imágenes con el servidor real: el límite de 2 MB lo aplica el parser multipart
 * del servidor, que MockMvc no usa. Las imágenes se guardan en una carpeta temporal, no en ./uploads.
 * Usa el Postgres de Docker ("docker compose up -d") para el login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImagenAdminIntegrationTests {

    private static final Path CARPETA = crearCarpetaTemporal();
    private static final String TIPO_INVALIDO = "La imagen tiene que ser JPG, PNG o WEBP.";

    @DynamicPropertySource
    static void carpetaDeSubidas(DynamicPropertyRegistry registro) {
        registro.add("alma.uploads.dir", CARPETA::toString);
    }

    @LocalServerPort
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private String admin;

    @BeforeEach
    void ingresar() throws Exception {
        HttpResponse<String> respuesta = cliente.send(HttpRequest.newBuilder(url("/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"nombreUsuario\":\"admin\",\"contrasenia\":\"Alma2026!\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        admin = JsonPath.read(respuesta.body(), "$.token");
    }

    @AfterAll
    static void borrarCarpeta() throws IOException {
        try (Stream<Path> archivos = Files.walk(CARPETA)) {
            for (Path archivo : archivos.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(archivo);
            }
        }
    }

    @Test
    void pngValidoSeGuardaYSePuedeVerSinToken() throws Exception {
        byte[] png = pngDePrueba();
        HttpResponse<String> respuesta = subir("foto.png", "image/png", png, admin);

        assertThat(respuesta.statusCode()).isEqualTo(201);
        String ruta = JsonPath.read(respuesta.body(), "$.ruta");
        assertThat(ruta).matches("/imagenes/[0-9a-f-]{36}\\.png");
        assertThat(CARPETA.resolve(ruta.substring("/imagenes/".length()))).exists();

        HttpResponse<byte[]> imagen = cliente.send(HttpRequest.newBuilder(url(ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(imagen.statusCode()).isEqualTo(200);
        assertThat(imagen.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(imagen.body()).isEqualTo(png);
    }

    @Test
    void textoConExtensionDeImagenDevuelve400() throws Exception {
        HttpResponse<String> respuesta = subir("falsa.png", "image/png",
                "no soy una imagen".getBytes(StandardCharsets.UTF_8), admin);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(respuesta.body(), "$.detail")).isEqualTo(TIPO_INVALIDO);
    }

    @Test
    void archivoVacioDevuelve400() throws Exception {
        HttpResponse<String> respuesta = subir("vacia.png", "image/png", new byte[0], admin);

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat((String) JsonPath.read(respuesta.body(), "$.detail")).isEqualTo(TIPO_INVALIDO);
    }

    @Test
    void archivoDeMasDe2MBDevuelve413() throws Exception {
        byte[] png = pngDePrueba();
        byte[] grande = new byte[2 * 1024 * 1024 + 100 * 1024];
        System.arraycopy(png, 0, grande, 0, png.length);

        HttpResponse<String> respuesta = subir("grande.png", "image/png", grande, admin);

        assertThat(respuesta.statusCode()).isEqualTo(413);
        assertThat((String) JsonPath.read(respuesta.body(), "$.detail"))
                .isEqualTo("La imagen no puede pesar más de 2 MB.");
    }

    private HttpResponse<String> subir(String nombre, String tipo, byte[] contenido, String token) throws Exception {
        String limite = "----alma" + UUID.randomUUID();
        ByteArrayOutputStream cuerpo = new ByteArrayOutputStream();
        cuerpo.write(("--" + limite + "\r\n"
                + "Content-Disposition: form-data; name=\"archivo\"; filename=\"" + nombre + "\"\r\n"
                + "Content-Type: " + tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(contenido);
        cuerpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest pedido = HttpRequest.newBuilder(url("/api/admin/imagenes"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + limite)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.toByteArray()))
                .build();
        return cliente.send(pedido, HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }

    private static byte[] pngDePrueba() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", salida);
        return salida.toByteArray();
    }

    private static Path crearCarpetaTemporal() {
        try {
            return Files.createTempDirectory("alma-imagenes-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
