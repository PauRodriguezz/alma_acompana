package ar.edu.utn.frm.gelab.alma.historia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Propiedades de {@code historia/historia.properties} (contrato del grafo, sección 10). El archivo se lee
 * como UTF-8, igual que {@code ConfiguracionChat}.
 */
@Configuration
@PropertySource(value = "classpath:historia/historia.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "alma.historia")
public class ConfiguracionHistoria {

    /** Tope de caracteres del texto de una respuesta (5.8). */
    private int textoMax = 1000;

    /** Temperatura de la llamada de extracción (5.4). */
    private double extraccionTemperatura = 0.1;

    /** Tope de tokens de salida de la llamada de extracción (5.4). */
    private int extraccionMaxTokens = 600;

    public double getExtraccionTemperatura() {
        return extraccionTemperatura;
    }

    public void setExtraccionTemperatura(double extraccionTemperatura) {
        this.extraccionTemperatura = extraccionTemperatura;
    }

    public int getExtraccionMaxTokens() {
        return extraccionMaxTokens;
    }

    public void setExtraccionMaxTokens(int extraccionMaxTokens) {
        this.extraccionMaxTokens = extraccionMaxTokens;
    }

    public int getTextoMax() {
        return textoMax;
    }

    public void setTextoMax(int textoMax) {
        this.textoMax = textoMax;
    }
}
