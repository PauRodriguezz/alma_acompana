package ar.edu.utn.frm.gelab.alma.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación, con la zona de Argentina. Se inyecta donde hace falta saber "ahora" para
 * poder reemplazarlo por uno fijo en las pruebas. No depende del TimeZone.setDefault de main().
 */
@Configuration
public class ClockConfig {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
