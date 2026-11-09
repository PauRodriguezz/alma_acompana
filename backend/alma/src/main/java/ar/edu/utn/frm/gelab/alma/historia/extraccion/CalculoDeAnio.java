package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;

/**
 * Año, precisión y etapa de vida de un recuerdo (contrato del grafo, 5.6). Funciones puras.
 *
 * <p>Orden: el año dicho (si no es anterior al nacimiento), la edad mencionada, la franja de edad de la
 * pregunta. Un año anterior al nacimiento se descarta y se sigue con lo que viene. Un año calculado que
 * quedaría en el futuro también se descarta.
 */
public final class CalculoDeAnio {

    /** {@code anio}, {@code precision} y {@code etapa} son nulos juntos: recuerdo sin año. */
    public record AnioDelRecuerdo(Integer anio, PrecisionAnio precision, EtapaDeVida etapa) {
        static final AnioDelRecuerdo SIN_ANIO = new AnioDelRecuerdo(null, null, null);
    }

    private CalculoDeAnio() {
    }

    public static AnioDelRecuerdo calcular(Integer anio, boolean aproximado, Integer edadMencionada,
            Integer edadDesde, Integer edadHasta, int anioDeNacimiento, int anioActual) {
        if (anio != null && anio >= anioDeNacimiento && anio <= anioActual) {
            return con(anio, aproximado ? PrecisionAnio.APROXIMADA : PrecisionAnio.EXACTA, anioDeNacimiento);
        }
        if (edadMencionada != null && anioDeNacimiento + edadMencionada <= anioActual) {
            return con(anioDeNacimiento + edadMencionada, PrecisionAnio.CALCULADA_POR_EDAD, anioDeNacimiento);
        }
        if (edadDesde != null && edadHasta != null) {
            int estimado = anioDeNacimiento + (edadDesde + edadHasta) / 2;
            if (estimado <= anioActual) {
                return con(estimado, PrecisionAnio.ESTIMADA, anioDeNacimiento);
            }
        }
        return AnioDelRecuerdo.SIN_ANIO;
    }

    /** Edad en el año del recuerdo: 0 a 11, 12 a 17, 18 a 29, 30 a 59 y 60 o más. */
    public static EtapaDeVida etapa(int edad) {
        if (edad <= 11) {
            return EtapaDeVida.INFANCIA;
        }
        if (edad <= 17) {
            return EtapaDeVida.ADOLESCENCIA;
        }
        if (edad <= 29) {
            return EtapaDeVida.JUVENTUD;
        }
        if (edad <= 59) {
            return EtapaDeVida.ADULTEZ;
        }
        return EtapaDeVida.VEJEZ;
    }

    private static AnioDelRecuerdo con(int anio, PrecisionAnio precision, int anioDeNacimiento) {
        return new AnioDelRecuerdo(anio, precision, etapa(anio - anioDeNacimiento));
    }
}
