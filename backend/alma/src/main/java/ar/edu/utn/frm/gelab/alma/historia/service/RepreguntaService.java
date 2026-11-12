package ar.edu.utn.frm.gelab.alma.historia.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.chat.ConfiguracionChat;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.TipoDeRiesgo;
import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import ar.edu.utn.frm.gelab.alma.common.exception.ConflictoException;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.historia.MensajesHistoria;
import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoRepregunta;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import ar.edu.utn.frm.gelab.alma.historia.dto.RepreguntaDto;
import ar.edu.utn.frm.gelab.alma.historia.dto.RepreguntaSiguienteResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaDelGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.TrabajoDeExtraccion;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo.ResultadoIntento;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaRepreguntar;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Repreguntas (contrato del grafo, 5.7 y endpoints 7.5 a 7.7): completan el año, los lugares y las personas de un
 * recuerdo ya creado. <b>No crean {@code PreguntaGuion} ni tocan el progreso.</b> La persona sale siempre del token
 * (el id de la cuenta). El log no lleva texto de la persona (5.9).
 */
@Service
public class RepreguntaService {

    private static final Logger log = LoggerFactory.getLogger(RepreguntaService.class);

    private final PersonaMayorRepository personaMayorRepository;
    private final LecturaDelGrafo lectura;
    private final EscrituraDelGrafo escritura;
    private final ColaDeExtracciones cola;
    private final ConfiguracionHistoria configuracion;
    private final DetectorDeRiesgo detector;
    private final ConfiguracionChat chat;

    public RepreguntaService(PersonaMayorRepository personaMayorRepository, LecturaDelGrafo lectura,
            EscrituraDelGrafo escritura, ColaDeExtracciones cola, ConfiguracionHistoria configuracion,
            DetectorDeRiesgo detector, ConfiguracionChat chat) {
        this.personaMayorRepository = personaMayorRepository;
        this.lectura = lectura;
        this.escritura = escritura;
        this.cola = cola;
        this.configuracion = configuracion;
        this.detector = detector;
        this.chat = chat;
    }

    /**
     * 7.5: ¿hay una repregunta candidata? {@code PROCESANDO} si la persona tiene una extracción en cola o en
     * curso (se mira <b>antes</b> de leer el grafo: si se mirara después, podría ofrecer una repregunta justo
     * antes de que esa extracción la cambie). Si no, la candidata del recuerdo más antiguo (o del que surgió de
     * {@code preguntaId}) con su primer dato candidato, sin los datos de {@code excluir}. Un {@code preguntaId}
     * que no existe o sin recuerdos da {@code NINGUNA}. Con {@code recuerdoId} (contrato del mapa de vida, 3.3), los
     * candidatos son solo los de ese recuerdo; uno que no existe o es de otra persona también da {@code NINGUNA}.
     * La repregunta {@code HAY} lleva el título del recuerdo.
     *
     * @throws RecursoNoEncontradoException 404: la cuenta no tiene persona mayor
     * @throws DatosInvalidosException      400: se mandaron {@code preguntaId} y {@code recuerdoId} a la vez
     */
    public RepreguntaSiguienteResponse siguiente(Long cuentaId, Integer preguntaId, UUID recuerdoId,
            List<DatoFaltante> excluir) {
        Long personaId = persona(cuentaId).getId();
        if (preguntaId != null && recuerdoId != null) {
            throw new DatosInvalidosException("recuerdoId", MensajesError.DATOS_INVALIDOS);
        }
        if (cola.estaProcesando(personaId)) {
            return new RepreguntaSiguienteResponse(EstadoRepregunta.PROCESANDO, null);
        }
        Set<DatoFaltante> excluidos = excluir == null || excluir.isEmpty() ? EnumSet.noneOf(DatoFaltante.class)
                : EnumSet.copyOf(excluir);
        List<RecuerdoParaRepreguntar> recuerdos = lectura.recuerdosParaRepreguntar(personaId, preguntaId,
                recuerdoId == null ? null : recuerdoId.toString());
        return SelectorDeRepregunta.siguiente(recuerdos, excluidos, configuracion.getIntentosMax())
                .map(c -> new RepreguntaSiguienteResponse(EstadoRepregunta.HAY, new RepreguntaDto(c.recuerdoId(),
                        c.dato(), MensajesHistoria.repregunta(c.dato()), tituloDe(recuerdos, c.recuerdoId()))))
                .orElseGet(() -> new RepreguntaSiguienteResponse(EstadoRepregunta.NINGUNA, null));
    }

    private static String tituloDe(List<RecuerdoParaRepreguntar> recuerdos, String recuerdoId) {
        return recuerdos.stream().filter(r -> r.id().equals(recuerdoId)).map(RecuerdoParaRepreguntar::titulo)
                .findFirst().orElse(null);
    }

    /**
     * 7.6: responde una repregunta. El orden es el de 5.1: persona, cuerpo y recién después el grafo. Cuenta el
     * intento (también si la respuesta no aporta el dato) y, ya confirmada esa transacción, encola la extracción:
     * si falla, el recuerdo no cambia pero el intento queda contado.
     *
     * @throws RecursoNoEncontradoException 404: sin persona mayor, o el recuerdo no existe o no es suyo
     * @throws DatosInvalidosException      400: la respuesta es más larga que el máximo
     * @throws ConflictoException           409: el dato no aplica a ese recuerdo, ya no falta o ya se preguntó el máximo
     */
    public RespuestaDelGuionResponse responder(Long cuentaId, String recuerdoId, DatoFaltante dato, String texto) {
        PersonaMayor persona = persona(cuentaId);
        if (texto.length() > configuracion.getTextoMax()) {
            throw new DatosInvalidosException("texto", MensajesHistoria.RESPUESTA_MUY_LARGA);
        }
        // Contrato de historia de vida, 5.6: con riesgo no se cuenta el intento, no se completa el dato y no se
        // encola nada (tampoco se valida que el recuerdo exista).
        Optional<TipoDeRiesgo> riesgo = detector.detectar(texto);
        if (riesgo.isPresent()) {
            return RespuestaDelGuionResponse.alarma(chat.getRespuestas().para(riesgo.get()));
        }
        registrarIntento(persona.getId(), recuerdoId, dato);
        // Contrato del grafo, 5.7: "no me acuerdo" no aporta nada. El intento se cuenta igual, pero no se le pide
        // nada al modelo (no hay pedido, ni espera ni riesgo de que invente).
        if (DetectorDeNoRecuerdo.esSinContenido(texto)) {
            log.info("Respuesta sin contenido de la persona {} a una repregunta: no se consulta al modelo",
                    persona.getId());
            return RespuestaDelGuionResponse.aceptada();
        }
        encolarExtraccion(persona, recuerdoId, dato, texto);
        return RespuestaDelGuionResponse.aceptada();
    }

    /**
     * 7.7: saltea una repregunta. Cuenta el intento igual que responder, no encola nada y no toca el recuerdo.
     * Mismos errores que {@link #responder}, salvo el del texto.
     */
    public void saltear(Long cuentaId, String recuerdoId, DatoFaltante dato) {
        registrarIntento(persona(cuentaId).getId(), recuerdoId, dato);
    }

    // ---------- Piezas ----------

    private PersonaMayor persona(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new);
    }

    private void registrarIntento(Long personaId, String recuerdoId, DatoFaltante dato) {
        ResultadoIntento resultado = escritura.registrarIntento(personaId, recuerdoId, dato,
                configuracion.getIntentosMax());
        switch (resultado) {
            case RECUERDO_INEXISTENTE -> throw new RecursoNoEncontradoException();
            case NO_CORRESPONDE -> throw new ConflictoException(MensajesHistoria.REPREGUNTA_YA_NO_HACE_FALTA);
            case REGISTRADO -> {
                // Nada más: el intento ya quedó contado.
            }
        }
    }

    /**
     * Lee ahora, en el hilo del pedido, lo que la extracción necesita de PostgreSQL y del grafo, para que el hilo de
     * la cola no toque JPA. El extractor recibe como "pregunta" el texto fijo de la repregunta, sin el título del
     * recuerdo: con el contexto el modelo copiaba al resultado lugares y personas que la persona no dijo
     * (contrato, v13). Nada de lo que pase acá cambia la respuesta de la persona.
     */
    private void encolarExtraccion(PersonaMayor persona, String recuerdoId, DatoFaltante dato, String texto) {
        try {
            PreguntaDelGuion origen = lectura.preguntaDeOrigen(persona.getId(), recuerdoId).orElseThrow();
            cola.encolar(new TrabajoDeExtraccion(persona.getId(), persona.getNombre(), persona.getApellido(),
                    persona.getFechaNacimiento(), origen.id(), MensajesHistoria.repregunta(dato), null, null, texto,
                    new TrabajoDeExtraccion.Repregunta(recuerdoId, dato)));
        } catch (RuntimeException e) {
            log.error("No se pudo encolar la extracción de una repregunta de la persona {} ({})", persona.getId(),
                    e.getClass().getSimpleName());
        }
    }
}
