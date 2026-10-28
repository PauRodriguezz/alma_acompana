package ar.edu.utn.frm.gelab.alma.common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * "Hoy" siempre es la fecha de Argentina, sin importar la zona del servidor.
 */
public final class Fechas {

    public static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA_ARGENTINA);
    }
}
