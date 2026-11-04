package ar.edu.utn.frm.gelab.alma.juegos.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;
import ar.edu.utn.frm.gelab.alma.juegos.MensajesJuego;
import ar.edu.utn.frm.gelab.alma.juegos.SinJuegosDisponiblesException;
import ar.edu.utn.frm.gelab.alma.juegos.domain.IntentoEjercicio;
import ar.edu.utn.frm.gelab.alma.juegos.domain.ProgresoJuego;
import ar.edu.utn.frm.gelab.alma.juegos.dto.EjercicioJuegoResponse;
import ar.edu.utn.frm.gelab.alma.juegos.dto.RespuestaRequest;
import ar.edu.utn.frm.gelab.alma.juegos.dto.ResultadoResponse;
import ar.edu.utn.frm.gelab.alma.juegos.dto.SiguienteResponse;
import ar.edu.utn.frm.gelab.alma.juegos.repository.IntentoEjercicioRepository;
import ar.edu.utn.frm.gelab.alma.juegos.repository.ProgresoJuegoRepository;
import ar.edu.utn.frm.gelab.alma.juegos.repository.UltimoIntento;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * El juego de la persona del token (CU-05): qué ejercicio sigue y cómo se registra la respuesta.
 * Contrato de juegos, secciones 5 y 6.
 */
@Service
public class JuegoService {

    private final PersonaMayorRepository personaMayorRepository;
    private final EjercicioRepository ejercicioRepository;
    private final ProgresoJuegoRepository progresoRepository;
    private final IntentoEjercicioRepository intentoRepository;
    private final SelectorDeEjercicio selector;
    private final Clock clock;

    public JuegoService(PersonaMayorRepository personaMayorRepository,
                        EjercicioRepository ejercicioRepository,
                        ProgresoJuegoRepository progresoRepository,
                        IntentoEjercicioRepository intentoRepository,
                        SelectorDeEjercicio selector,
                        Clock clock) {
        this.personaMayorRepository = personaMayorRepository;
        this.ejercicioRepository = ejercicioRepository;
        this.progresoRepository = progresoRepository;
        this.intentoRepository = intentoRepository;
        this.selector = selector;
        this.clock = clock;
    }

    /**
     * Próximo ejercicio (5.1) con las opciones mezcladas y la posición en la ronda (5.7).
     * No escribe nada: sin progreso se parte de BASICO y sin último ejercicio.
     */
    @Transactional(readOnly = true)
    public SiguienteResponse siguiente(Long cuentaId) {
        PersonaMayor persona = personaDe(cuentaId);
        ProgresoJuego progreso = progresoRepository.findByPersonaMayorId(persona.getId()).orElse(null);
        NivelDificultad nivel = progreso != null ? progreso.getNivelActual() : NivelDificultad.BASICO;
        Ejercicio ultimo = progreso != null ? progreso.getUltimoEjercicio() : null;

        Map<Long, LocalDateTime> ultimaVez = intentoRepository.ultimoIntentoPorEjercicio(persona.getId()).stream()
                .collect(Collectors.toMap(UltimoIntento::ejercicioId, UltimoIntento::fechaHora));

        Ejercicio elegido = selector
                .elegir(nivel, ultimo, ejercicioRepository::findByNivelDificultadAndFechaBajaIsNull, ultimaVez)
                .orElseThrow(SinJuegosDisponiblesException::new);

        List<OpcionRespuesta> opciones = selector.mezclar(elegido.getOpciones(), elegido.getFormato());
        long intentosDeHoy = intentosDeHoy(persona, LocalDate.now(clock));
        int posicion = (int) (intentosDeHoy % MensajesJuego.TOTAL_RONDA) + 1;
        return new SiguienteResponse(EjercicioJuegoResponse.de(elegido, opciones), posicion,
                MensajesJuego.TOTAL_RONDA);
    }

    /**
     * Valida la respuesta, guarda el intento y actualiza el progreso (5.4), todo en una transacción.
     * Acepta ejercicios dados de baja (si se dieron de baja mientras la persona jugaba).
     */
    @Transactional
    public ResultadoResponse responder(Long cuentaId, RespuestaRequest datos) {
        PersonaMayor persona = personaDe(cuentaId);
        Ejercicio ejercicio = ejercicioRepository.findById(datos.ejercicioId())
                .orElseThrow(RecursoNoEncontradoException::new);
        validar(ejercicio, datos);

        boolean acierto = esAcierto(ejercicio, datos);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        // Se cuentan antes de guardar: así no depende de cuándo Hibernate manda el insert.
        long jugadosHoy = intentosDeHoy(persona, hoy) + 1;

        IntentoEjercicio intento = new IntentoEjercicio();
        intento.setPersonaMayor(persona);
        intento.setEjercicio(ejercicio);
        intento.setFechaHora(ahora);
        intento.setAcierto(acierto);
        intentoRepository.save(intento);

        ProgresoJuego progreso = progresoRepository.findByPersonaMayorId(persona.getId()).orElseGet(() -> {
            ProgresoJuego nuevo = new ProgresoJuego();
            nuevo.setPersonaMayor(persona);
            return nuevo;
        });
        if (acierto) {
            progreso.registrarAcierto();
        } else {
            progreso.registrarError();
        }
        progreso.setUltimoEjercicio(ejercicio);
        progreso.setFechaUltimaSesion(hoy);
        progresoRepository.save(progreso);

        return new ResultadoResponse(acierto, ejercicio.getExplicacion(), respuestaCorrecta(ejercicio),
                jugadosHoy % MensajesJuego.TOTAL_RONDA == 0, jugadosHoy);
    }

    private PersonaMayor personaDe(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new);
    }

    /** Intentos con fechaHora entre las 00:00 del día y las 00:00 del siguiente (Argentina). */
    private long intentosDeHoy(PersonaMayor persona, LocalDate hoy) {
        return intentoRepository.countByPersonaMayorIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                persona.getId(), hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
    }

    /**
     * Sección 5.6. El error va siempre en el campo del formato del ejercicio: opcionId en opción múltiple
     * y ordenOpcionIds en ordenar, también cuando sobra el campo del otro formato.
     */
    private static void validar(Ejercicio ejercicio, RespuestaRequest datos) {
        Set<Long> idsDelEjercicio = ejercicio.getOpciones().stream()
                .map(OpcionRespuesta::getId)
                .collect(Collectors.toSet());
        List<Long> orden = datos.ordenOpcionIds();

        if (ejercicio.getFormato() == FormatoEjercicio.OPCION_MULTIPLE) {
            boolean valido = datos.opcionId() != null
                    && idsDelEjercicio.contains(datos.opcionId())
                    && (orden == null || orden.isEmpty());
            if (!valido) {
                throw new DatosInvalidosException("opcionId", MensajesJuego.OPCION_OBLIGATORIA);
            }
        } else {
            boolean valido = datos.opcionId() == null
                    && orden != null
                    && orden.size() == idsDelEjercicio.size()
                    && new HashSet<>(orden).equals(idsDelEjercicio);
            if (!valido) {
                throw new DatosInvalidosException("ordenOpcionIds", MensajesJuego.ORDEN_INCOMPLETO);
            }
        }
    }

    /** Opción múltiple: la elegida es la correcta. Ordenar: coincide posición por posición. */
    private static boolean esAcierto(Ejercicio ejercicio, RespuestaRequest datos) {
        if (ejercicio.getFormato() == FormatoEjercicio.OPCION_MULTIPLE) {
            return ejercicio.getOpciones().stream()
                    .anyMatch(o -> o.getId().equals(datos.opcionId()) && Boolean.TRUE.equals(o.getEsCorrecta()));
        }
        List<Long> correcto = SelectorDeEjercicio.ordenCorrecto(ejercicio.getOpciones()).stream()
                .map(OpcionRespuesta::getId)
                .toList();
        return correcto.equals(datos.ordenOpcionIds());
    }

    private static List<String> respuestaCorrecta(Ejercicio ejercicio) {
        List<OpcionRespuesta> opciones = ejercicio.getFormato() == FormatoEjercicio.OPCION_MULTIPLE
                ? ejercicio.getOpciones().stream().filter(o -> Boolean.TRUE.equals(o.getEsCorrecta())).toList()
                : SelectorDeEjercicio.ordenCorrecto(ejercicio.getOpciones());
        return opciones.stream().map(OpcionRespuesta::getTexto).toList();
    }
}
