package ar.edu.utn.frm.gelab.alma.historia.config;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;

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

    /** Máximo de veces que se pregunta por cada dato de un recuerdo (5.7). */
    private int intentosMax = 2;

    /** Temperatura de la llamada de extracción (5.4). */
    private double extraccionTemperatura = 0.1;

    /** Tope de tokens de salida de la llamada de extracción (5.4). */
    private int extraccionMaxTokens = 800;

    /** Espera, en milisegundos, antes de repetir un pedido que falló por un error transitorio del proveedor (5.4). */
    private long extraccionReintentoEsperaMs = 1500;

    /**
     * Un error transitorio solo se reintenta si desde el primer pedido de la extracción pasaron como mucho estos
     * milisegundos (5.4): cada llamada ya tiene su tope de 25 s, y así el total no se alarga sin límite.
     */
    private long extraccionReintentoHastaMs = 25000;

    /** Largo máximo del resumen de un recuerdo en el listado (contrato de historia de vida, 7.3). */
    private int resumenMax = 140;

    public long getExtraccionReintentoEsperaMs() {
        return extraccionReintentoEsperaMs;
    }

    public void setExtraccionReintentoEsperaMs(long extraccionReintentoEsperaMs) {
        this.extraccionReintentoEsperaMs = extraccionReintentoEsperaMs;
    }

    public long getExtraccionReintentoHastaMs() {
        return extraccionReintentoHastaMs;
    }

    public void setExtraccionReintentoHastaMs(long extraccionReintentoHastaMs) {
        this.extraccionReintentoHastaMs = extraccionReintentoHastaMs;
    }

    public int getResumenMax() {
        return resumenMax;
    }

    public void setResumenMax(int resumenMax) {
        this.resumenMax = resumenMax;
    }

    /** Máximo de recuerdos que se agregan al prompt del chat (contrato de historia de vida, 5.5 y 10.5). */
    private int maxRecuerdosPrompt = 3;

    /** Largo máximo de la descripción de cada recuerdo en el prompt del chat (5.5.2). */
    private int descripcionPromptMax = 300;

    /** Frases de etapa de vida que traen recuerdos de esa etapa al prompt (5.5.1). Se normalizan al usarlas. */
    private Map<EtapaDeVida, List<String>> frasesEtapa = new EnumMap<>(EtapaDeVida.class);

    /**
     * Tope de tiempo, en milisegundos, de la lectura del grafo para el prompt del chat (progreso y recuerdos juntos).
     * Si se pasa, el chat sigue con "No hay información." sin esperar más.
     */
    private long lecturaChatTimeoutMs = 2000;

    /** Máximo de lecturas del grafo para el chat que corren a la vez. Con todas ocupadas, se rechaza de inmediato. */
    private int lecturaChatHilosMax = 4;

    public long getLecturaChatTimeoutMs() {
        return lecturaChatTimeoutMs;
    }

    public void setLecturaChatTimeoutMs(long lecturaChatTimeoutMs) {
        this.lecturaChatTimeoutMs = lecturaChatTimeoutMs;
    }

    public int getLecturaChatHilosMax() {
        return lecturaChatHilosMax;
    }

    public void setLecturaChatHilosMax(int lecturaChatHilosMax) {
        this.lecturaChatHilosMax = lecturaChatHilosMax;
    }

    public int getMaxRecuerdosPrompt() {
        return maxRecuerdosPrompt;
    }

    public void setMaxRecuerdosPrompt(int maxRecuerdosPrompt) {
        this.maxRecuerdosPrompt = maxRecuerdosPrompt;
    }

    public int getDescripcionPromptMax() {
        return descripcionPromptMax;
    }

    public void setDescripcionPromptMax(int descripcionPromptMax) {
        this.descripcionPromptMax = descripcionPromptMax;
    }

    public Map<EtapaDeVida, List<String>> getFrasesEtapa() {
        return frasesEtapa;
    }

    public void setFrasesEtapa(Map<EtapaDeVida, List<String>> frasesEtapa) {
        this.frasesEtapa = frasesEtapa;
    }

    public int getIntentosMax() {
        return intentosMax;
    }

    public void setIntentosMax(int intentosMax) {
        this.intentosMax = intentosMax;
    }

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
