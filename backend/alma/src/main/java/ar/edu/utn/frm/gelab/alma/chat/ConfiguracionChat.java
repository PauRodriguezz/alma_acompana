package ar.edu.utn.frm.gelab.alma.chat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ar.edu.utn.frm.gelab.alma.chat.guardrails.TipoDeRiesgo;

/**
 * Propiedades de {@code chat/chat.properties} (contrato de chat, sección 2): respuestas predefinidas,
 * listas de expresiones y límites. El archivo se lee como UTF-8. Las propiedades {@code alma.ia.*} del
 * mismo archivo (tiempos, temperatura y tope de tokens) las lee {@code ia/} con {@code @Value}.
 */
@Configuration
@PropertySource(value = "classpath:chat/chat.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "alma.chat")
public class ConfiguracionChat {

    private final Respuestas respuestas = new Respuestas();
    private final Limites limites = new Limites();
    private final Guardrails guardrails = new Guardrails();
    private final Citas citas = new Citas();

    public Citas getCitas() {
        return citas;
    }

    public Respuestas getRespuestas() {
        return respuestas;
    }

    public Limites getLimites() {
        return limites;
    }

    public Guardrails getGuardrails() {
        return guardrails;
    }

    public static class Respuestas {

        private String urgencia;
        private String malestarGrave;

        /** El texto predefinido para un riesgo detectado (lo usan el chat y las respuestas del guion). */
        public String para(TipoDeRiesgo riesgo) {
            return switch (riesgo) {
                case MALESTAR_GRAVE -> malestarGrave;
                case URGENCIA_MEDICA -> urgencia;
            };
        }

        public String getUrgencia() {
            return urgencia;
        }

        public void setUrgencia(String urgencia) {
            this.urgencia = urgencia;
        }

        public String getMalestarGrave() {
            return malestarGrave;
        }

        public void setMalestarGrave(String malestarGrave) {
            this.malestarGrave = malestarGrave;
        }
    }

    /** Citas médicas próximas que recibe el modelo (contrato, 5.4): hasta cuántos días y cuántas como máximo. */
    public static class Citas {

        private int ventanaDias = 14;
        private int maximo = 10;

        public int getVentanaDias() {
            return ventanaDias;
        }

        public void setVentanaDias(int ventanaDias) {
            this.ventanaDias = ventanaDias;
        }

        public int getMaximo() {
            return maximo;
        }

        public void setMaximo(int maximo) {
            this.maximo = maximo;
        }
    }

    public static class Limites {

        private int mensajeMax = 500;
        private int textoConversacionMax = 2000;
        private int turnosMax = 10;

        public int getMensajeMax() {
            return mensajeMax;
        }

        public void setMensajeMax(int mensajeMax) {
            this.mensajeMax = mensajeMax;
        }

        public int getTextoConversacionMax() {
            return textoConversacionMax;
        }

        public void setTextoConversacionMax(int textoConversacionMax) {
            this.textoConversacionMax = textoConversacionMax;
        }

        public int getTurnosMax() {
            return turnosMax;
        }

        public void setTurnosMax(int turnosMax) {
            this.turnosMax = turnosMax;
        }
    }

    public static class Guardrails {

        private List<String> malestar = new ArrayList<>();
        private List<String> urgencias = new ArrayList<>();

        public List<String> getMalestar() {
            return malestar;
        }

        public void setMalestar(List<String> malestar) {
            this.malestar = malestar;
        }

        public List<String> getUrgencias() {
            return urgencias;
        }

        public void setUrgencias(List<String> urgencias) {
            this.urgencias = urgencias;
        }
    }
}
