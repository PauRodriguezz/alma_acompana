package ar.edu.utn.frm.gelab.alma.historia.service;

import static ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionNeo4j.MANEJADOR_NEO4J;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.chat.ConfiguracionChat;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.DetectorDeRiesgo;
import ar.edu.utn.frm.gelab.alma.chat.guardrails.TipoDeRiesgo;
import ar.edu.utn.frm.gelab.alma.common.exception.ConflictoException;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.historia.MensajesHistoria;
import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.EstadoPregunta;
import ar.edu.utn.frm.gelab.alma.historia.domain.PreguntaDelGuion;
import ar.edu.utn.frm.gelab.alma.historia.domain.TipoProgreso;
import ar.edu.utn.frm.gelab.alma.historia.dto.EstadoGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.PreguntaDto;
import ar.edu.utn.frm.gelab.alma.historia.dto.PreguntaEstadoDto;
import ar.edu.utn.frm.gelab.alma.historia.dto.ProximaPreguntaResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RespuestaDelGuionResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.UltimaRegistradaDto;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.ColaDeExtracciones;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.TrabajoDeExtraccion;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo.ResultadoRegistro;
import ar.edu.utn.frm.gelab.alma.historia.repository.EstadoDePregunta;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Historia de vida de la persona mayor: guion y progreso (contrato del grafo, secciones 5.1 a 5.3 y 5.8).
 * La persona sale siempre del token (el id de la cuenta); el id de la persona mayor es el mismo en
 * PostgreSQL y en el grafo. El log no lleva texto de la persona (5.9).
 */
@Service
public class HistoriaDeVidaService {

    private static final Logger log = LoggerFactory.getLogger(HistoriaDeVidaService.class);

    private static final String RESPONDIO = TipoProgreso.RESPONDIO.name();
    private static final String SALTEO = TipoProgreso.SALTEO.name();

    private final PersonaMayorRepository personaMayorRepository;
    private final EscrituraDelGrafo escritura;
    private final LecturaDelGrafo lectura;
    private final ConfiguracionHistoria configuracion;
    private final ColaDeExtracciones cola;
    private final DetectorDeRiesgo detector;
    private final ConfiguracionChat chat;

    public HistoriaDeVidaService(PersonaMayorRepository personaMayorRepository, EscrituraDelGrafo escritura,
            LecturaDelGrafo lectura, ConfiguracionHistoria configuracion, ColaDeExtracciones cola,
            DetectorDeRiesgo detector, ConfiguracionChat chat) {
        this.personaMayorRepository = personaMayorRepository;
        this.escritura = escritura;
        this.lectura = lectura;
        this.configuracion = configuracion;
        this.cola = cola;
        this.detector = detector;
        this.chat = chat;
    }

    /**
     * La próxima pregunta pendiente (P1: la de menor {@code orden} sin {@code RESPONDIO} ni {@code SALTEO})
     * con su introducción (5.2). Hace el {@code MERGE} del nodo de la persona, así que escribe.
     */
    @Transactional(MANEJADOR_NEO4J)
    public ProximaPreguntaResponse proxima(Long cuentaId) {
        List<EstadoDePregunta> todas = leerEstado(personaId(cuentaId));
        Optional<EstadoDePregunta> proxima = todas.stream().filter(e -> e.tipo() == null).findFirst();
        if (proxima.isEmpty()) {
            return new ProximaPreguntaResponse(null, null, MensajesHistoria.MENSAJE_FINAL, false);
        }
        EstadoDePregunta pregunta = proxima.get();
        return new ProximaPreguntaResponse(new PreguntaDto(pregunta.id(), pregunta.orden(), pregunta.texto()),
                introduccion(pregunta, ultimaRegistrada(todas)), null, true);
    }

    /** Todas las preguntas con su estado (P4) y la última respondida o salteada (P3). Escribe: ver {@link #proxima}. */
    @Transactional(MANEJADOR_NEO4J)
    public EstadoGuionResponse estado(Long cuentaId) {
        List<EstadoDePregunta> todas = leerEstado(personaId(cuentaId));
        List<PreguntaEstadoDto> preguntas = todas.stream()
                .map(e -> new PreguntaEstadoDto(e.id(), e.orden(), e.texto(), estadoDe(e))).toList();
        int respondidas = (int) preguntas.stream().filter(p -> p.estado() == EstadoPregunta.RESPONDIDA).count();
        int salteadas = (int) preguntas.stream().filter(p -> p.estado() == EstadoPregunta.SALTEADA).count();
        UltimaRegistradaDto ultima = ultimaRegistrada(todas)
                .map(e -> new UltimaRegistradaDto(e.id(), estadoDe(e), e.fecha().toOffsetDateTime()))
                .orElse(null);
        return new EstadoGuionResponse(respondidas, salteadas, preguntas.size() - respondidas - salteadas,
                preguntas, ultima);
    }

    /**
     * Registra la respuesta (5.1). El orden es el del contrato: persona, cuerpo, y recién después se toca el
     * grafo, así que un pedido inválido no crea nada en Neo4j. La pregunta queda respondida al instante y, ya
     * confirmada esa transacción, se encola la extracción del recuerdo (5.4): si encolar fallara, la respuesta
     * sigue registrada. Si la respuesta solo dice que no recuerda o no sabe ({@link DetectorDeNoRecuerdo}), la
     * pregunta queda <b>salteada</b> y no se encola nada; la respuesta HTTP es la misma (202 ACEPTADA).
     *
     * @throws RecursoNoEncontradoException 404: la cuenta no tiene persona mayor o la pregunta no existe
     * @throws DatosInvalidosException      400: la respuesta es más larga que el máximo
     * @throws ConflictoException           409: la pregunta ya estaba respondida o salteada
     */
    public RespuestaDelGuionResponse responder(Long cuentaId, int preguntaId, String texto) {
        PersonaMayor persona = persona(cuentaId);
        if (texto.length() > configuracion.getTextoMax()) {
            throw new DatosInvalidosException("texto", MensajesHistoria.RESPUESTA_MUY_LARGA);
        }
        // Contrato de historia de vida, 5.6: si salta el detector de riesgo no se toca nada (ni se valida que la
        // pregunta exista o siga pendiente): la pregunta sigue pendiente y no se encola la extracción.
        Optional<TipoDeRiesgo> riesgo = detector.detectar(texto);
        if (riesgo.isPresent()) {
            return RespuestaDelGuionResponse.alarma(chat.getRespuestas().para(riesgo.get()));
        }
        // Contrato del grafo, 5.1: una respuesta que solo dice que no recuerda, no sabe o no quiere contar no crea un
        // recuerdo: se registra como un salteo (la pregunta no se vuelve a ofrecer) y no se encola nada.
        if (DetectorDeNoRecuerdo.esSinContenido(texto)) {
            registrar(persona.getId(), preguntaId, TipoProgreso.SALTEO);
            log.info("Respuesta sin contenido de la persona {}: se registra como salteo, sin recuerdo",
                    persona.getId());
            return RespuestaDelGuionResponse.aceptada();
        }
        registrar(persona.getId(), preguntaId, TipoProgreso.RESPONDIO);
        encolarExtraccion(persona, preguntaId, texto);
        return RespuestaDelGuionResponse.aceptada();
    }

    /** Registra el salteo (5.3): no valida texto y no toca el grafo de recuerdos. Mismos errores que responder. */
    public void saltear(Long cuentaId, int preguntaId) {
        registrar(personaId(cuentaId), preguntaId, TipoProgreso.SALTEO);
    }

    /**
     * Baja de la persona (contrato, 5.10): borra su nodo y todos sus recuerdos, personas y lugares, sin tocar
     * las preguntas del guion ni los datos de otras personas. Se puede repetir sin error. Todavía no la llama
     * ningún endpoint: hoy el panel de operador no da de baja personas (contrato, sección 2).
     */
    @Transactional(MANEJADOR_NEO4J)
    public void eliminarHistoria(Long personaId) {
        escritura.eliminarSubgrafo(personaId);
    }

    // ---------- Piezas ----------

    private PersonaMayor persona(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new);
    }

    private Long personaId(Long cuentaId) {
        return persona(cuentaId).getId();
    }

    /**
     * Lee ahora, en el hilo del pedido, todo lo que la extracción necesita de PostgreSQL y del guion, para que el
     * hilo de la cola no toque JPA. Nada de lo que pase acá cambia la respuesta de la persona.
     */
    private void encolarExtraccion(PersonaMayor persona, int preguntaId, String texto) {
        try {
            PreguntaDelGuion pregunta = lectura.pregunta(preguntaId).orElseThrow();
            cola.encolar(new TrabajoDeExtraccion(persona.getId(), persona.getNombre(), persona.getApellido(),
                    persona.getFechaNacimiento(), pregunta.id(), pregunta.texto(), pregunta.edadDesde(),
                    pregunta.edadHasta(), texto));
        } catch (RuntimeException e) {
            log.error("No se pudo encolar la extracción de la persona {} ({})", persona.getId(),
                    e.getClass().getSimpleName());
        }
    }

    private List<EstadoDePregunta> leerEstado(Long personaId) {
        escritura.asegurarPersona(personaId);
        return lectura.estadoDeTodas(personaId);
    }

    private void registrar(Long personaId, int preguntaId, TipoProgreso tipo) {
        ResultadoRegistro resultado = escritura.registrarProgreso(personaId, preguntaId, tipo);
        switch (resultado) {
            case PREGUNTA_INEXISTENTE -> throw new RecursoNoEncontradoException();
            case YA_REGISTRADA -> throw new ConflictoException(MensajesHistoria.PREGUNTA_YA_REGISTRADA);
            case REGISTRADO -> {
                // Nada más por ahora.
            }
        }
    }

    /** P3: la respondida o salteada más reciente. Con la misma fecha desempata la de mayor orden. */
    private static Optional<EstadoDePregunta> ultimaRegistrada(List<EstadoDePregunta> todas) {
        return todas.stream().filter(e -> e.tipo() != null)
                .max(Comparator.comparing((EstadoDePregunta e) -> e.fecha().toInstant())
                        .thenComparing(EstadoDePregunta::orden));
    }

    /** 5.2: primera vez, después de un salteo, o una de las tres frases según el orden (orden % 3). */
    private static String introduccion(EstadoDePregunta proxima, Optional<EstadoDePregunta> ultima) {
        if (ultima.isEmpty()) {
            return MensajesHistoria.INTRODUCCION_PRIMERA_VEZ;
        }
        if (SALTEO.equals(ultima.get().tipo())) {
            return MensajesHistoria.INTRODUCCION_DESPUES_DE_SALTEO;
        }
        return MensajesHistoria.INTRODUCCIONES_NORMALES[proxima.orden() % MensajesHistoria.INTRODUCCIONES_NORMALES.length];
    }

    private static EstadoPregunta estadoDe(EstadoDePregunta fila) {
        if (fila.tipo() == null) {
            return EstadoPregunta.PENDIENTE;
        }
        return RESPONDIO.equals(fila.tipo()) ? EstadoPregunta.RESPONDIDA : EstadoPregunta.SALTEADA;
    }
}
