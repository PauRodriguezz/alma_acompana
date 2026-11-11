package ar.edu.utn.frm.gelab.alma.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.chat.ConfiguracionChat;
import ar.edu.utn.frm.gelab.alma.chat.MensajesChat;
import ar.edu.utn.frm.gelab.alma.chat.dto.MensajeChatRequest;
import ar.edu.utn.frm.gelab.alma.chat.dto.RespuestaChatResponse;
import ar.edu.utn.frm.gelab.alma.chat.dto.RolChat;
import ar.edu.utn.frm.gelab.alma.chat.dto.SaludoResponse;
import ar.edu.utn.frm.gelab.alma.chat.dto.TurnoRequest;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.TipoDeRiesgo;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.ia.LimpiadorDeRespuesta;
import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import ar.edu.utn.frm.gelab.alma.ia.TurnoConversacion;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;
import ar.edu.utn.frm.gelab.alma.recordatorios.service.RecordatorioService;

/**
 * Saludo y respuesta del chat con Alma (CU-02, contrato de chat). No guarda nada de la conversación y no
 * escribe en el log el texto de los mensajes (5.8).
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PersonaMayorRepository personaRepository;
    private final RecordatorioService recordatorioService;
    private final DetectorDeRiesgo detector;
    private final ContextoDelChat contexto;
    private final ModeloConversacional modelo;
    private final ConfiguracionChat configuracion;
    private final Clock clock;

    public ChatService(PersonaMayorRepository personaRepository, RecordatorioService recordatorioService,
            DetectorDeRiesgo detector, ContextoDelChat contexto, ModeloConversacional modelo,
            ConfiguracionChat configuracion, Clock clock) {
        this.personaRepository = personaRepository;
        this.recordatorioService = recordatorioService;
        this.detector = detector;
        this.contexto = contexto;
        this.modelo = modelo;
        this.configuracion = configuracion;
        this.clock = clock;
    }

    /** Saludo "{franja}, {nombre}" y mensaje según la agenda de hoy (5.1). */
    public SaludoResponse saludar(Long cuentaId) {
        PersonaMayor persona = persona(cuentaId);
        String saludo = ContextoDelChat.franjaDelSaludo(LocalTime.now(clock)) + ", " + persona.getNombre();
        String mensaje;
        try {
            LocalDate hoy = LocalDate.now(clock);
            mensaje = ContextoDelChat.mensajeDeSaludo(recordatorioService.agenda(cuentaId, hoy, hoy, null).size());
        } catch (RuntimeException e) {
            // Si falla la agenda, el saludo sale igual sin mención a recordatorios.
            log.warn("No se pudo leer la agenda para el saludo ({})", e.getClass().getSimpleName());
            mensaje = MensajesChat.SALUDO_SIN_RECORDATORIOS;
        }
        return new SaludoResponse(saludo, mensaje);
    }

    /** Recorrido de un mensaje (5.2). */
    public RespuestaChatResponse responder(Long cuentaId, MensajeChatRequest datos) {
        String mensaje = datos.mensaje().strip();
        if (mensaje.length() > configuracion.getLimites().getMensajeMax()) {
            throw new DatosInvalidosException("mensaje", MensajesChat.MENSAJE_MUY_LARGO);
        }
        persona(cuentaId);

        // Solo el mensaje nuevo se compara con las listas, no la conversación anterior (5.2).
        Optional<TipoDeRiesgo> riesgo = detector.detectar(mensaje);
        if (riesgo.isPresent()) {
            return new RespuestaChatResponse(configuracion.getRespuestas().para(riesgo.get()), true);
        }

        String instrucciones = contexto.instrucciones(cuentaId, mensaje);
        // El modelo falso de las pruebas no limpia: la limpieza se repite acá, es inofensiva.
        String respuesta = LimpiadorDeRespuesta.limpiar(modelo.responder(instrucciones, turnos(datos, mensaje)));
        if (respuesta.isEmpty()) {
            throw new ModeloNoDisponibleException("El modelo devolvió una respuesta vacía");
        }
        return new RespuestaChatResponse(respuesta, false);
    }

    /** Los últimos N turnos (cada texto cortado) y, al final, el mensaje nuevo (5.5). */
    private List<TurnoConversacion> turnos(MensajeChatRequest datos, String mensaje) {
        int maxTurnos = configuracion.getLimites().getTurnosMax();
        int maxTexto = configuracion.getLimites().getTextoConversacionMax();
        List<TurnoRequest> anteriores = datos.conversacion() == null ? List.of() : datos.conversacion();
        List<TurnoRequest> ultimos = anteriores.subList(Math.max(0, anteriores.size() - maxTurnos),
                anteriores.size());
        List<TurnoConversacion> turnos = new ArrayList<>();
        for (TurnoRequest turno : ultimos) {
            String texto = turno.texto().length() > maxTexto ? turno.texto().substring(0, maxTexto) : turno.texto();
            turnos.add(new TurnoConversacion(
                    turno.rol() == RolChat.ALMA ? TurnoConversacion.Rol.ALMA : TurnoConversacion.Rol.PERSONA,
                    texto));
        }
        turnos.add(new TurnoConversacion(TurnoConversacion.Rol.PERSONA, mensaje));
        return turnos;
    }

    private PersonaMayor persona(Long cuentaId) {
        return personaRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new);
    }
}
