package ar.edu.utn.frm.gelab.alma.historia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Pone el {@link ModeloDePrueba} en lugar de Ollama (es {@code @Primary}), para las pruebas que responden
 * preguntas del guion y por lo tanto encolan extracciones. Se importa con {@code @Import}.
 */
@TestConfiguration
class ModeloDePruebaConfig {

    @Bean
    @Primary
    ModeloDePrueba modeloDePrueba() {
        return new ModeloDePrueba();
    }
}
