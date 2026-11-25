package ar.edu.utn.frm.gelab.alma.microcapsulas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.config.DatosDeCatalogo;
import ar.edu.utn.frm.gelab.alma.config.DatosDeCatalogoMicrocapsulas;
import ar.edu.utn.frm.gelab.alma.config.MicrocapsulaSemilla;
import ar.edu.utn.frm.gelab.alma.config.TextosDeMicrocapsulas;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaRequest;

/**
 * Las 10 microcápsulas del seed cumplen las reglas del panel (título hasta 120, texto hasta 5000, formato
 * permitido), con el mismo validador que usa MicrocapsulaService. No usa Spring ni las bases de datos.
 */
class TextosDeSeedTest {

    static List<MicrocapsulaSemilla> semillas() {
        return Stream.concat(DatosDeCatalogo.SEMILLAS.stream(), DatosDeCatalogoMicrocapsulas.SEMILLAS.stream())
                .toList();
    }

    @Test
    void hayDiezMicrocapsulasConTituloYArchivoUnicos() {
        List<MicrocapsulaSemilla> semillas = semillas();

        assertThat(semillas).hasSize(10);
        assertThat(semillas.stream().map(MicrocapsulaSemilla::titulo)).doesNotHaveDuplicates();
        assertThat(semillas.stream().map(MicrocapsulaSemilla::archivo)).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("semillas")
    void cumpleLosLimitesYElFormatoPermitido(MicrocapsulaSemilla semilla) {
        String texto = TextosDeMicrocapsulas.leer(semilla.archivo());
        MicrocapsulaRequest pedido = new MicrocapsulaRequest(semilla.titulo(), texto, semilla.categoria(), null,
                Fechas.hoy());

        try {
            MicrocapsulaService.validar(pedido);
        } catch (DatosInvalidosException e) {
            fail("«%s» no pasa el validador (título %d, texto %d caracteres): %s",
                    semilla.titulo(), semilla.titulo().strip().length(), texto.strip().length(), e.getErrores());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("semillas")
    void elArchivoEstaEnUtf8SinBomYConSaltosLf(MicrocapsulaSemilla semilla) throws IOException {
        byte[] bytes;
        try (InputStream entrada = new ClassPathResource("microcapsulas/" + semilla.archivo()).getInputStream()) {
            bytes = entrada.readAllBytes();
        }

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isNotEqualTo((byte) 0xEF);
        assertThat(bytes).doesNotContain((byte) '\r');
    }
}
