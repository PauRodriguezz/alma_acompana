package ar.edu.utn.frm.gelab.alma.juegos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;
import ar.edu.utn.frm.gelab.alma.juegos.SinJuegosDisponiblesException;
import ar.edu.utn.frm.gelab.alma.juegos.controller.JuegoController;
import ar.edu.utn.frm.gelab.alma.juegos.repository.IntentoEjercicioRepository;
import ar.edu.utn.frm.gelab.alma.juegos.repository.ProgresoJuegoRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * "Sin ejercicios activos → 404" (contrato de juegos, 5.1 y 8), sin base: los repositorios son simulados,
 * así no hace falta dar de baja el catálogo real.
 */
class JuegoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Long CUENTA = 1L;

    private EjercicioRepository ejercicioRepository;
    private ProgresoJuegoRepository progresoRepository;
    private IntentoEjercicioRepository intentoRepository;
    private JuegoService servicio;

    @BeforeEach
    void preparar() {
        PersonaMayorRepository personaMayorRepository = mock(PersonaMayorRepository.class);
        ejercicioRepository = mock(EjercicioRepository.class);
        progresoRepository = mock(ProgresoJuegoRepository.class);
        intentoRepository = mock(IntentoEjercicioRepository.class);

        PersonaMayor persona = new PersonaMayor();
        persona.setId(10L);
        when(personaMayorRepository.findByCuentaId(CUENTA)).thenReturn(Optional.of(persona));
        when(progresoRepository.findByPersonaMayorId(10L)).thenReturn(Optional.empty());
        when(intentoRepository.ultimoIntentoPorEjercicio(10L)).thenReturn(List.of());
        when(ejercicioRepository.findByNivelDificultadAndFechaBajaIsNull(any())).thenReturn(List.of());

        Clock reloj = Clock.fixed(LocalDateTime.of(2026, 11, 4, 10, 0).atZone(ZONA).toInstant(), ZONA);
        servicio = new JuegoService(personaMayorRepository, ejercicioRepository, progresoRepository,
                intentoRepository, new SelectorDeEjercicio(), reloj);
    }

    @Test
    void sinEjerciciosActivosEnNingunNivelTiraSinJuegosDisponibles() {
        assertThatThrownBy(() -> servicio.siguiente(CUENTA)).isInstanceOf(SinJuegosDisponiblesException.class);

        // Probó los tres niveles y no escribió nada.
        for (NivelDificultad nivel : NivelDificultad.values()) {
            verify(ejercicioRepository).findByNivelDificultadAndFechaBajaIsNull(nivel);
        }
        verify(progresoRepository, never()).save(any());
        verify(intentoRepository, never()).save(any());
    }

    @Test
    void elControladorLoConvierteEn404ConElTextoDelContrato() {
        JuegoController controlador = new JuegoController(servicio);
        MockHttpServletRequest pedido = new MockHttpServletRequest("GET", "/api/juegos/siguiente");

        ProblemDetail problema = controlador.sinJuegos(new SinJuegosDisponiblesException(), pedido);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getTitle()).isEqualTo("No encontrado");
        assertThat(problema.getDetail()).isEqualTo("Por ahora no hay juegos disponibles. Probá más tarde.");
        assertThat(problema.getInstance()).hasToString("/api/juegos/siguiente");
    }
}
