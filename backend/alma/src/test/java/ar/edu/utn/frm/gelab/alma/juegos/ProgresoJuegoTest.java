package ar.edu.utn.frm.gelab.alma.juegos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.juegos.domain.ProgresoJuego;

/**
 * Nivel y contadores de ProgresoJuego (contrato de juegos, 5.5). Sin base de datos.
 */
class ProgresoJuegoTest {

    private static ProgresoJuego progreso(NivelDificultad nivel) {
        ProgresoJuego progreso = new ProgresoJuego();
        progreso.setNivelActual(nivel);
        return progreso;
    }

    private static void verificar(ProgresoJuego progreso, NivelDificultad nivel, int aciertos, int errores) {
        assertThat(progreso.getNivelActual()).isEqualTo(nivel);
        assertThat(progreso.getAciertosConsecutivos()).isEqualTo(aciertos);
        assertThat(progreso.getErroresConsecutivos()).isEqualTo(errores);
    }

    @Test
    void nuevoEmpiezaEnBasicoConContadoresEnCero() {
        verificar(new ProgresoJuego(), NivelDificultad.BASICO, 0, 0);
    }

    @Test
    void dosAciertosNoSuben() {
        ProgresoJuego progreso = progreso(NivelDificultad.BASICO);
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        verificar(progreso, NivelDificultad.BASICO, 2, 0);
    }

    @Test
    void tresAciertosSeguidosSubenYReinician() {
        ProgresoJuego progreso = progreso(NivelDificultad.BASICO);
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        verificar(progreso, NivelDificultad.INTERMEDIO, 0, 0);
    }

    @Test
    void deIntermedioSubeAAvanzado() {
        ProgresoJuego progreso = progreso(NivelDificultad.INTERMEDIO);
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        verificar(progreso, NivelDificultad.AVANZADO, 0, 0);
    }

    @Test
    void enAvanzadoNoSubeMasPeroReinicia() {
        ProgresoJuego progreso = progreso(NivelDificultad.AVANZADO);
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        verificar(progreso, NivelDificultad.AVANZADO, 0, 0);
    }

    @Test
    void unErrorNoBaja() {
        ProgresoJuego progreso = progreso(NivelDificultad.INTERMEDIO);
        progreso.registrarError();
        verificar(progreso, NivelDificultad.INTERMEDIO, 0, 1);
    }

    @Test
    void dosErroresSeguidosBajanYReinician() {
        ProgresoJuego progreso = progreso(NivelDificultad.AVANZADO);
        progreso.registrarError();
        progreso.registrarError();
        verificar(progreso, NivelDificultad.INTERMEDIO, 0, 0);
    }

    @Test
    void deIntermedioBajaABasico() {
        ProgresoJuego progreso = progreso(NivelDificultad.INTERMEDIO);
        progreso.registrarError();
        progreso.registrarError();
        verificar(progreso, NivelDificultad.BASICO, 0, 0);
    }

    @Test
    void enBasicoNoBajaMasPeroReinicia() {
        ProgresoJuego progreso = progreso(NivelDificultad.BASICO);
        progreso.registrarError();
        progreso.registrarError();
        verificar(progreso, NivelDificultad.BASICO, 0, 0);
    }

    @Test
    void unErrorCortaLaRachaDeAciertos() {
        ProgresoJuego progreso = progreso(NivelDificultad.BASICO);
        progreso.registrarAcierto();
        progreso.registrarAcierto();
        progreso.registrarError();
        verificar(progreso, NivelDificultad.BASICO, 0, 1);
        progreso.registrarAcierto();
        verificar(progreso, NivelDificultad.BASICO, 1, 0);
    }

    @Test
    void unAciertoCortaLaRachaDeErrores() {
        ProgresoJuego progreso = progreso(NivelDificultad.INTERMEDIO);
        progreso.registrarError();
        progreso.registrarAcierto();
        progreso.registrarError();
        verificar(progreso, NivelDificultad.INTERMEDIO, 0, 1);
    }

    @Test
    void despuesDeSubirHacenFaltaOtrosTresAciertos() {
        ProgresoJuego progreso = progreso(NivelDificultad.BASICO);
        for (int i = 0; i < 5; i++) {
            progreso.registrarAcierto();
        }
        verificar(progreso, NivelDificultad.INTERMEDIO, 2, 0);
    }
}
