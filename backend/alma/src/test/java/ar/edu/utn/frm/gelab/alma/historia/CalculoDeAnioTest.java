package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.CalculoDeAnio;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.CalculoDeAnio.AnioDelRecuerdo;

/** Año, precisión y etapa de vida (contrato del grafo, 5.6). Persona nacida en 1950, "hoy" es 2026. */
class CalculoDeAnioTest {

    private static final int NACIMIENTO = 1950;
    private static final int HOY = 2026;

    private static AnioDelRecuerdo calcular(Integer anio, boolean aproximado, Integer edad, Integer desde,
            Integer hasta) {
        return CalculoDeAnio.calcular(anio, aproximado, edad, desde, hasta, NACIMIENTO, HOY);
    }

    @Test
    void anioExactoEsExacta() {
        AnioDelRecuerdo r = calcular(1957, false, null, 6, 12);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1957, PrecisionAnio.EXACTA, EtapaDeVida.INFANCIA));
    }

    @Test
    void anioAproximadoEsAproximada() {
        AnioDelRecuerdo r = calcular(1958, true, null, null, null);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1958, PrecisionAnio.APROXIMADA, EtapaDeVida.INFANCIA));
    }

    @Test
    void sinAnioPeroConEdadSeCalculaPorEdad() {
        AnioDelRecuerdo r = calcular(null, false, 17, 14, 25);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1967, PrecisionAnio.CALCULADA_POR_EDAD, EtapaDeVida.ADOLESCENCIA));
    }

    @Test
    void sinAnioNiEdadSeEstimaPorLaFranjaConDivisionEntera() {
        // (6 + 12) / 2 = 9 -> 1959.
        AnioDelRecuerdo r = calcular(null, false, null, 6, 12);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1959, PrecisionAnio.ESTIMADA, EtapaDeVida.INFANCIA));
        // (4 + 11) / 2 = 7 (entera).
        assertThat(calcular(null, false, null, 4, 11).anio()).isEqualTo(1957);
    }

    @Test
    void sinNadaYSinFranjaQuedaSinAnioPrecisionNiEtapa() {
        assertThat(calcular(null, false, null, null, null)).isEqualTo(new AnioDelRecuerdo(null, null, null));
    }

    @Test
    void unAnioAnteriorAlNacimientoSeDescartaYSigueConLaEdad() {
        AnioDelRecuerdo r = calcular(1940, false, 20, 6, 12);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1970, PrecisionAnio.CALCULADA_POR_EDAD, EtapaDeVida.JUVENTUD));
    }

    @Test
    void unAnioAnteriorAlNacimientoSinEdadSigueConLaFranja() {
        AnioDelRecuerdo r = calcular(1940, false, null, 6, 12);
        assertThat(r).isEqualTo(new AnioDelRecuerdo(1959, PrecisionAnio.ESTIMADA, EtapaDeVida.INFANCIA));
    }

    @Test
    void unAnioAnteriorAlNacimientoSinEdadNiFranjaQuedaSinAnio() {
        assertThat(calcular(1940, false, null, null, null)).isEqualTo(new AnioDelRecuerdo(null, null, null));
    }

    @Test
    void elAnioDeNacimientoEsValido() {
        assertThat(calcular(1950, false, null, null, null).anio()).isEqualTo(1950);
    }

    @Test
    void unAnioCalculadoQueQuedariaEnElFuturoSeDescarta() {
        // 1950 + 100 = 2050 > 2026: no se acepta; sin franja, sin año.
        assertThat(calcular(null, false, 100, null, null)).isEqualTo(new AnioDelRecuerdo(null, null, null));
    }

    @ParameterizedTest
    @CsvSource({ "0,INFANCIA", "11,INFANCIA", "12,ADOLESCENCIA", "17,ADOLESCENCIA", "18,JUVENTUD", "29,JUVENTUD",
            "30,ADULTEZ", "59,ADULTEZ", "60,VEJEZ", "95,VEJEZ" })
    void laEtapaDeVidaEnLosLimites(int edad, EtapaDeVida esperada) {
        assertThat(CalculoDeAnio.etapa(edad)).isEqualTo(esperada);
        // Y el mismo límite pasando por el cálculo completo.
        assertThat(CalculoDeAnio.calcular(NACIMIENTO + edad, false, null, null, null, NACIMIENTO, 2100).etapa())
                .isEqualTo(esperada);
    }
}
