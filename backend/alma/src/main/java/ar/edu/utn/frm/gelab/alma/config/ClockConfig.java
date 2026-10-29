package ar.edu.utn.frm.gelab.alma.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ar.edu.utn.frm.gelab.alma.common.Fechas;

/**
 * Reloj de la aplicación, con la zona de Argentina (la de {@link Fechas#ZONA_ARGENTINA}). Se inyecta
 * donde hace falta saber "ahora" para poder reemplazarlo por uno fijo en las pruebas. No depende del
 * TimeZone.setDefault de main().
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(Fechas.ZONA_ARGENTINA);
    }
}
