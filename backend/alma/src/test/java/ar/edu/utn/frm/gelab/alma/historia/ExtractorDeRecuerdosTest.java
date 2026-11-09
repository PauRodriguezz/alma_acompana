package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ExtractorDeRecuerdos;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.PersonaExtraida;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.RespuestaExtraida;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ResultadoDeExtraccion;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.TrabajoDeExtraccion;
import tools.jackson.databind.json.JsonMapper;

/**
 * El extractor (contrato del grafo, 5.4) con el modelo de prueba, sin Spring y sin bases: limpieza,
 * conversión, validación de estructura y de contenido, reintento, respaldo y privacidad del log.
 * Persona: Ana Pérez, nacida en 1950; "hoy" es el 9/11/2026.
 */
@ExtendWith(OutputCaptureExtension.class)
class ExtractorDeRecuerdosTest {

    private static final String PREGUNTA = "¿Qué te acordás de tu escuela primaria?";
    private static final String RESPUESTA = "En 1957 empecé primer grado en la Escuela 12. Mi maestra Elena me enseñó a leer.";
    private static final String VALIDO = """
            {"titulo":"Mi maestra Elena","descripcion":"Su maestra de primer grado.","anio":1957,\
            "anioAproximado":false,"edadMencionada":null,"personas":[{"nombre":"Elena","vinculo":"maestra"}],\
            "lugares":["Escuela 12"]}""";

    private ModeloDePrueba modelo;
    private ExtractorDeRecuerdos extractor;

    @BeforeEach
    void preparar() {
        modelo = new ModeloDePrueba();
        Clock reloj = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        extractor = new ExtractorDeRecuerdos(modelo, new ConfiguracionHistoria(), reloj, JsonMapper.builder().build());
    }

    private static TrabajoDeExtraccion trabajo(String respuesta) {
        return new TrabajoDeExtraccion(1L, "Ana", "Pérez", LocalDate.of(1950, 5, 12), 7, PREGUNTA, 6, 12, respuesta);
    }

    private ResultadoDeExtraccion extraer(String salida, String respuesta) {
        modelo.responde(salida);
        return extractor.extraer(trabajo(respuesta));
    }

    // ---------- Respuesta válida y pedido ----------

    @Test
    void unaRespuestaValidaSeConvierteConUnSoloPedido() {
        ResultadoDeExtraccion r = extraer(VALIDO, RESPUESTA);

        assertThat(r.respaldo()).isFalse();
        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.respuesta().titulo()).isEqualTo("Mi maestra Elena");
        assertThat(r.respuesta().anio()).isEqualTo(1957);
        assertThat(r.respuesta().personas()).containsExactly(new PersonaExtraida("Elena", "maestra"));
        assertThat(r.respuesta().lugares()).containsExactly("Escuela 12");
    }

    @Test
    void elPedidoLlevaElPromptLaPreguntaLaRespuestaYLosParametrosDeLaExtraccion() {
        extraer(VALIDO, RESPUESTA);

        ModeloDePrueba.Pedido pedido = modelo.pedidos().get(0);
        assertThat(pedido.instrucciones()).contains("Devolvés SOLO un objeto JSON", "No asumas el género");
        assertThat(pedido.mensaje()).contains(PREGUNTA).contains(RESPUESTA);
        assertThat(pedido.opciones().temperatura()).isEqualTo(0.1);
        assertThat(pedido.opciones().maxTokens()).isEqualTo(600);
    }

    @Test
    void laReglaDeLasDecadasDelPromptDiceQueUnaDecadaNoEsUnAnio() {
        extraer(VALIDO, RESPUESTA);

        String prompt = modelo.pedidos().get(0).instrucciones();
        assertThat(prompt).contains("Una década").contains("NO es un año").contains("\"anio\" va null");
        assertThat(prompt).doesNotContain("va el primer año de la década");
    }

    // ---------- Limpieza y conversión ----------

    @Test
    void conTextoAntesYDespuesDelJsonSeCorrige() {
        ResultadoDeExtraccion r = extraer("Acá está: " + VALIDO + " Espero que sirva.", RESPUESTA);
        assertThat(r.respaldo()).isFalse();
        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.respuesta().titulo()).isEqualTo("Mi maestra Elena");
    }

    @Test
    void conDelimitadoresDeCodigoSeCorrige() {
        ResultadoDeExtraccion r = extraer("```json\n" + VALIDO + "\n```", RESPUESTA);
        assertThat(r.respaldo()).isFalse();
        assertThat(r.pedidos()).isEqualTo(1);
    }

    @Test
    void unAnioEscritoComoTextoPasaANumero() {
        ResultadoDeExtraccion r = extraer(VALIDO.replace("\"anio\":1957", "\"anio\":\"1957\""), RESPUESTA);
        assertThat(r.respaldo()).isFalse();
        assertThat(r.respuesta().anio()).isEqualTo(1957);
    }

    @Test
    void elTextoNullPasaANulo() {
        String salida = VALIDO.replace("\"anio\":1957", "\"anio\":\"null\"")
                .replace("\"edadMencionada\":null", "\"edadMencionada\":\"null\"")
                .replace("\"nombre\":\"Elena\"", "\"nombre\":\"null\"");
        ResultadoDeExtraccion r = extraer(salida, RESPUESTA);
        assertThat(r.respaldo()).isFalse();
        assertThat(r.respuesta().anio()).isNull();
        assertThat(r.respuesta().edadMencionada()).isNull();
        assertThat(r.respuesta().personas()).containsExactly(new PersonaExtraida(null, "maestra"));
    }

    @Test
    void unCampoQueFaltaSeCompletaConListaVaciaONulo() {
        ResultadoDeExtraccion r = extraer("{\"titulo\":\"Algo\",\"descripcion\":\"Contó algo.\"}", "Algo");
        assertThat(r.respaldo()).isFalse();
        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.respuesta().lugares()).isEmpty();
        assertThat(r.respuesta().personas()).isEmpty();
        assertThat(r.respuesta().anio()).isNull();
        assertThat(r.respuesta().anioAproximado()).isFalse();
    }

    // ---------- Validación de estructura (reintenta) ----------

    @Test
    void unAnioFueraDeRangoReintentaConElErrorEnElPedido() {
        ResultadoDeExtraccion r = extractor.extraer(conRespuestas(VALIDO.replace("1957", "1850"), VALIDO));

        assertThat(r.respaldo()).isFalse();
        assertThat(r.pedidos()).isEqualTo(2);
        assertThat(r.respuesta().anio()).isEqualTo(1957);
        assertThat(modelo.pedidos().get(1).mensaje()).contains("anio debe estar entre 1900 y 2026");
        assertThat(modelo.pedidos().get(0).mensaje()).doesNotContain("no se pudo usar");
    }

    @Test
    void unAnioEnElFuturoUnaEdadFueraDeRangoYUnTituloVacioReintentan() {
        for (String roto : new String[] { VALIDO.replace("1957", "2027"),
                VALIDO.replace("\"edadMencionada\":null", "\"edadMencionada\":150"),
                VALIDO.replace("\"Mi maestra Elena\"", "\"  \"") }) {
            modelo.reiniciar();
            ResultadoDeExtraccion r = extractor.extraer(conRespuestas(roto, VALIDO));
            assertThat(r.pedidos()).as(roto).isEqualTo(2);
            assertThat(r.respaldo()).as(roto).isFalse();
        }
    }

    @Test
    void unTipoIncorrectoReintenta() {
        ResultadoDeExtraccion r = extractor.extraer(
                conRespuestas(VALIDO.replace("\"lugares\":[\"Escuela 12\"]", "\"lugares\":\"Escuela 12\""), VALIDO));
        assertThat(r.pedidos()).isEqualTo(2);
        assertThat(r.respaldo()).isFalse();
    }

    // ---------- Validación de contenido (descarta, no reintenta) ----------

    @Test
    void unAnioQueNoApareceEnElTextoSeDescartaSinReintentar() {
        ResultadoDeExtraccion r = extraer(VALIDO, "Mi maestra Elena me enseñó a leer.");

        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.respaldo()).isFalse();
        assertThat(r.respuesta().anio()).isNull();
        assertThat(r.respuesta().anioAproximado()).isFalse();
    }

    @Test
    void unaEdadQueNoApareceEnElTextoSeDescarta() {
        String salida = VALIDO.replace("\"anio\":1957", "\"anio\":null")
                .replace("\"edadMencionada\":null", "\"edadMencionada\":17");

        assertThat(extraer(salida, "Trabajé en una panadería.").respuesta().edadMencionada()).isNull();
        modelo.reiniciar();
        assertThat(extraer(salida, "Trabajé en una panadería cuando tenía 17 años.").respuesta().edadMencionada())
                .isEqualTo(17);
    }

    @Test
    void unNumeroDentroDeOtroNoCuentaComoEscritoEnCifras() {
        // "1957" no está en "11957" ni en "19570": no se acepta.
        assertThat(extraer(VALIDO, "Fue el 11957 o el 19570.").respuesta().anio()).isNull();
    }

    @Test
    void unaDecadaEscritaNoSeConvierteEnAnio() {
        // E3: el modelo interpreta "los años 50" como 1950 aproximado, pero "1950" no está en el texto.
        String salida = VALIDO.replace("\"anio\":1957", "\"anio\":1950").replace("\"anioAproximado\":false",
                "\"anioAproximado\":true");

        RespuestaExtraida r = extraer(salida, "Por los años 50, en Cipolletti, en una casa con patio.").respuesta();

        assertThat(r.anio()).isNull();
        assertThat(r.anioAproximado()).isFalse();
    }

    @Test
    void laPropiaPersonaMayorNoEntraComoPersona() {
        String salida = VALIDO.replace("[{\"nombre\":\"Elena\",\"vinculo\":\"maestra\"}]",
                "[{\"nombre\":\"Ana\",\"vinculo\":\"yo\"},{\"nombre\":\"ANA PÉREZ\",\"vinculo\":\"yo\"},"
                        + "{\"nombre\":\"Marta\",\"vinculo\":\"amiga\"},{\"nombre\":\"marta\",\"vinculo\":\"amiga\"},"
                        + "{\"nombre\":null,\"vinculo\":\"tío\"}]");

        RespuestaExtraida r = extraer(salida, "Me llamo Ana y salía con mis amigas Marta y Rosa.").respuesta();

        assertThat(r.personas()).extracting(PersonaExtraida::nombre).containsExactly("Marta", null);
    }

    // ---------- Respaldo ----------

    @Test
    void conElJsonInvalidoDosVecesVaElRespaldoConDosPedidos() {
        ResultadoDeExtraccion r = extractor.extraer(conRespuestas("no es json", "{ tampoco"));

        assertThat(r.respaldo()).isTrue();
        assertThat(r.pedidos()).isEqualTo(2);
        assertThat(r.respuesta()).isEqualTo(new RespuestaExtraida(PREGUNTA, RESPUESTA, null, false, null,
                java.util.List.of(), java.util.List.of()));
    }

    @Test
    void siFallaElModeloVaElRespaldoSinReintentar() {
        modelo.falla();

        ResultadoDeExtraccion r = extractor.extraer(trabajo(RESPUESTA));

        assertThat(r.respaldo()).isTrue();
        assertThat(r.pedidos()).isEqualTo(1);
        assertThat(r.respuesta().titulo()).isEqualTo(PREGUNTA);
        assertThat(r.respuesta().descripcion()).isEqualTo(RESPUESTA);
    }

    @Test
    void siFallaElModeloEnElReintentoVaElRespaldo() {
        modelo.responde("{").falla();

        ResultadoDeExtraccion r = extractor.extraer(trabajo(RESPUESTA));

        assertThat(r.respaldo()).isTrue();
        assertThat(r.pedidos()).isEqualTo(2);
    }

    @Test
    void unErrorInesperadoNoSaleHaciaAfuera() {
        ExtractorDeRecuerdos conError = new ExtractorDeRecuerdos(
                (instrucciones, turnos) -> {
                    throw new IllegalStateException("explotó con " + RESPUESTA);
                }, new ConfiguracionHistoria(), Clock.systemDefaultZone(), JsonMapper.builder().build());

        ResultadoDeExtraccion r = conError.extraer(trabajo(RESPUESTA));

        assertThat(r.respaldo()).isTrue();
    }

    // ---------- Privacidad (5.9) ----------

    @Test
    void elLogNoTraeElTextoDeLaPersonaNiElDelModelo(CapturedOutput salida) {
        String secreto = "SECRETO_DE_LA_PERSONA";
        ResultadoDeExtraccion ok = extraer(VALIDO.replace("Mi maestra Elena", "TITULO_DEL_MODELO"), secreto + " 1957");
        modelo.reiniciar();
        modelo.responde("SALIDA_ROTA_DEL_MODELO").responde("{ SALIDA_ROTA_2");
        ResultadoDeExtraccion roto = extractor.extraer(trabajo(secreto));
        modelo.reiniciar();
        modelo.falla();
        extractor.extraer(trabajo(secreto));

        assertThat(ok.respaldo()).isFalse();
        assertThat(roto.respaldo()).isTrue();
        assertThat(salida.getAll()).contains("Extracción:");
        assertThat(salida.getAll()).doesNotContain(secreto).doesNotContain("TITULO_DEL_MODELO")
                .doesNotContain("SALIDA_ROTA").doesNotContain("Elena").doesNotContain(PREGUNTA);
    }

    // ---------- Utilidades ----------

    /** Programa las respuestas y devuelve el trabajo normal. */
    private TrabajoDeExtraccion conRespuestas(String primera, String segunda) {
        modelo.responde(primera).responde(segunda);
        return trabajo(RESPUESTA);
    }
}
