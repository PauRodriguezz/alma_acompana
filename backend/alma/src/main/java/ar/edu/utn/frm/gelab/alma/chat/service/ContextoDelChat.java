package ar.edu.utn.frm.gelab.alma.chat.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import ar.edu.utn.frm.gelab.alma.bienestar.domain.NivelAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.service.RegistroAnimoService;
import ar.edu.utn.frm.gelab.alma.chat.ConfiguracionChat;
import ar.edu.utn.frm.gelab.alma.chat.MensajesChat;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.ItemAgendaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.MedicacionResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.service.RecordatorioService;

/**
 * Arma lo que el modelo recibe como instrucciones: el system prompt con los marcadores ya reemplazados
 * (contrato de chat, 5.4), y la franja del saludo. El modelo no recibe nombre, usuario ni datos de la cuenta.
 */
@Component
public class ContextoDelChat {

    private static final Pattern MARCADOR = Pattern
            .compile("\\{(respuesta_urgencia|respuesta_malestar_grave|animo|recordatorios|progreso|recuerdos)}");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final String plantilla;
    private final ConfiguracionChat configuracion;
    private final RegistroAnimoService animoService;
    private final RecordatorioService recordatorioService;
    private final Clock clock;

    public ContextoDelChat(ConfiguracionChat configuracion, RegistroAnimoService animoService,
            RecordatorioService recordatorioService, Clock clock) {
        this.configuracion = configuracion;
        this.animoService = animoService;
        this.recordatorioService = recordatorioService;
        this.clock = clock;
        this.plantilla = leerPlantilla();
    }

    /** "Buen día" de 06:00 a 11:59, "Buenas tardes" de 12:00 a 19:59 y "Buenas noches" el resto (5.1). */
    public static String franjaDelSaludo(LocalTime hora) {
        int h = hora.getHour();
        if (h >= 6 && h < 12) {
            return MensajesChat.BUEN_DIA;
        }
        if (h >= 12 && h < 20) {
            return MensajesChat.BUENAS_TARDES;
        }
        return MensajesChat.BUENAS_NOCHES;
    }

    public static String mensajeDeSaludo(int cantidadDeRecordatorios) {
        if (cantidadDeRecordatorios == 0) {
            return MensajesChat.SALUDO_SIN_RECORDATORIOS;
        }
        return cantidadDeRecordatorios == 1
                ? MensajesChat.SALUDO_UN_RECORDATORIO
                : MensajesChat.SALUDO_VARIOS_RECORDATORIOS;
    }

    /** El system prompt con el ánimo y los recordatorios de hoy de esa cuenta. */
    public String instrucciones(Long cuentaId) {
        LocalDate hoy = LocalDate.now(clock);
        Map<String, String> valores = Map.of(
                "respuesta_urgencia", configuracion.getRespuestas().getUrgencia(),
                "respuesta_malestar_grave", configuracion.getRespuestas().getMalestarGrave(),
                "animo", animoDeHoy(cuentaId),
                "recordatorios", recordatoriosDeHoy(cuentaId, hoy),
                "progreso", MensajesChat.SIN_INFORMACION,
                "recuerdos", MensajesChat.SIN_INFORMACION);
        // Una sola pasada sobre la plantilla: lo que se inserta nunca se vuelve a examinar, así un título
        // de recordatorio con algo como "{animo}" no se reemplaza.
        Matcher matcher = MARCADOR.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(resultado, Matcher.quoteReplacement(valores.get(matcher.group(1))));
        }
        matcher.appendTail(resultado);
        return resultado.toString();
    }

    private String animoDeHoy(Long cuentaId) {
        return animoService.animoDeHoy(cuentaId)
                .map(registro -> etiqueta(registro.nivelAnimo()))
                .orElse(MensajesChat.ANIMO_SIN_REGISTRO);
    }

    private static String etiqueta(NivelAnimo nivel) {
        return switch (nivel) {
            case MUY_MAL -> "Muy mal";
            case MAL -> "Mal";
            case NEUTRO -> "Más o menos";
            case BIEN -> "Bien";
            case MUY_BIEN -> "Muy bien";
        };
    }

    private String recordatoriosDeHoy(Long cuentaId, LocalDate hoy) {
        List<ItemAgendaResponse> agenda = recordatorioService.agenda(cuentaId, hoy, hoy, null);
        if (agenda.isEmpty()) {
            return MensajesChat.SIN_RECORDATORIOS;
        }
        return agenda.stream()
                .sorted(Comparator.comparing(ItemAgendaResponse::fechaHoraProgramada))
                .map(ContextoDelChat::linea)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(MensajesChat.SIN_RECORDATORIOS);
    }

    /** Sin médico, lugar ni indicaciones previas (5.4). */
    private static String linea(ItemAgendaResponse item) {
        String hora = item.fechaHoraProgramada().format(HORA);
        String estado = estado(item);
        return switch (item.recordatorio()) {
            case MedicacionResponse m -> "- " + hora + ", medicación: "
                    + unir(m.nombreMedicamento(), m.dosis()) + " (" + estado + ")";
            case CitaResponse c -> "- " + hora + ", cita médica: "
                    + unir(c.titulo(), c.especialidad()) + " (" + estado + ")";
        };
    }

    private static String estado(ItemAgendaResponse item) {
        return switch (item.estado()) {
            case PENDIENTE -> item.atrasada() ? "pendiente, ya pasó la hora" : "pendiente";
            case TOMADA -> "ya tomada";
            case VISTA -> "ya vista";
        };
    }

    private static String unir(String principal, String secundario) {
        return secundario == null || secundario.isBlank() ? principal : principal + ", " + secundario;
    }

    private static String leerPlantilla() {
        try (var entrada = new ClassPathResource("chat/system-prompt.txt").getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer chat/system-prompt.txt", e);
        }
    }
}
