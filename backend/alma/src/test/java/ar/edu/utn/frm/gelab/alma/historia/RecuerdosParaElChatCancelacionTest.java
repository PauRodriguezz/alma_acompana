package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.repository.EstadoDePregunta;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.NombreConocido;
import ar.edu.utn.frm.gelab.alma.historia.service.RecuerdosParaElChat;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Una lectura cancelada por el tope no se registra como ERROR, pero un fallo real sí. El driver o Spring Data pueden
 * limpiar la marca de interrupción antes del {@code catch}: por eso estas pruebas simulan un driver que, al ser
 * interrumpido, <b>no</b> restaura la marca y lanza la excepción que traduce Spring Data. Lo que las salva es la
 * bandera propia de {@link RecuerdosParaElChat}. Sin base de datos.
 */
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class RecuerdosParaElChatCancelacionTest {

    private static final Long CUENTA = 1L;
    private static final long PERSONA = 7L;
    private static final String SIN_INFORMACION = "No hay información.";
    private static final String MENSAJE_SECRETO = "MENSAJE-SECRETO-DEL-DRIVER";
    private static final List<EstadoDePregunta> UNA_RESPONDIDA = List.of(
            new EstadoDePregunta(1, 1, "Uno", "RESPONDIO", ZonedDateTime.now()),
            new EstadoDePregunta(2, 2, "Dos", null, null));

    private final LecturaDelGrafo lectura = mock(LecturaDelGrafo.class);

    private RecuerdosParaElChat servicio(long topeMs) {
        PersonaMayorRepository repositorio = mock(PersonaMayorRepository.class);
        PersonaMayor persona = mock(PersonaMayor.class);
        when(persona.getId()).thenReturn(PERSONA);
        when(repositorio.findByCuentaId(CUENTA)).thenReturn(Optional.of(persona));
        ConfiguracionHistoria configuracion = new ConfiguracionHistoria();
        configuracion.setLecturaChatHilosMax(2);
        configuracion.setLecturaChatTimeoutMs(topeMs);
        return new RecuerdosParaElChat(repositorio, lectura, configuracion);
    }

    /**
     * Un driver que se cuelga y, al interrumpirlo, lanza la excepción de Spring Data sin dejar la marca puesta (una
     * {@code InterruptedException} atrapada la limpia, y acá no se restaura).
     */
    private static <T> Answer<T> colgadaQueFallaAlCancelar() {
        return invocacion -> {
            try {
                new CountDownLatch(1).await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // La marca queda limpia a propósito.
            }
            throw new InvalidDataAccessResourceUsageException(MENSAJE_SECRETO);
        };
    }

    private static void esperarEnElLog(CapturedOutput salida, String texto) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < limite && !(salida.getOut() + salida.getErr()).contains(texto)) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        assertThat(salida.getOut() + salida.getErr()).as("el log tiene que mostrar: " + texto).contains(texto);
    }

    private static String log(CapturedOutput salida) {
        return salida.getOut() + salida.getErr();
    }

    @Test
    void unaLecturaDeProgresoCanceladaPorElTopeNoSeRegistraComoError(CapturedOutput salida) {
        when(lectura.estadoDeTodas(PERSONA)).thenAnswer(colgadaQueFallaAlCancelar());

        RecuerdosParaElChat.Resultado resultado = servicio(300).armar(CUENTA, "Hola, ¿cómo andás?");

        assertThat(resultado.progreso()).isEqualTo(SIN_INFORMACION);
        assertThat(resultado.recuerdos()).isEqualTo(SIN_INFORMACION);
        esperarEnElLog(salida, "cancelada por el tope");
        assertThat(log(salida)).contains("superó el tope de 300 ms").doesNotContain("No se pudo leer el progreso")
                .doesNotContain(MENSAJE_SECRETO);
    }

    @Test
    void unaLecturaDeRecuerdosCanceladaPorElTopeNoSeRegistraComoError(CapturedOutput salida) {
        when(lectura.estadoDeTodas(PERSONA)).thenReturn(UNA_RESPONDIDA);
        when(lectura.personasConocidas(PERSONA)).thenAnswer(colgadaQueFallaAlCancelar());

        RecuerdosParaElChat.Resultado resultado = servicio(300).armar(CUENTA, "Hoy me acordé de Elena");

        assertThat(resultado.recuerdos()).isEqualTo(SIN_INFORMACION);
        esperarEnElLog(salida, "cancelada por el tope");
        assertThat(log(salida)).doesNotContain("No se pudieron leer los recuerdos").doesNotContain(MENSAJE_SECRETO);
    }

    @Test
    void unFalloRealDelProgresoSigueSiendoError(CapturedOutput salida) {
        when(lectura.estadoDeTodas(PERSONA)).thenThrow(new InvalidDataAccessResourceUsageException(MENSAJE_SECRETO));

        RecuerdosParaElChat.Resultado resultado = servicio(5000).armar(CUENTA, "Hola");

        assertThat(resultado.progreso()).isEqualTo(SIN_INFORMACION);
        assertThat(log(salida)).contains("ERROR")
                .contains("No se pudo leer el progreso para el chat (InvalidDataAccessResourceUsageException)")
                .doesNotContain("cancelada por el tope").doesNotContain(MENSAJE_SECRETO);
    }

    @Test
    void unFalloRealDeLosRecuerdosSigueSiendoError(CapturedOutput salida) {
        when(lectura.estadoDeTodas(PERSONA)).thenReturn(UNA_RESPONDIDA);
        when(lectura.personasConocidas(PERSONA)).thenThrow(new InvalidDataAccessResourceUsageException(MENSAJE_SECRETO));

        RecuerdosParaElChat.Resultado resultado = servicio(5000).armar(CUENTA, "Hoy me acordé de Elena");

        // El progreso se arma igual: cada parte falla por separado.
        assertThat(resultado.progreso()).startsWith("Respondió");
        assertThat(resultado.recuerdos()).isEqualTo(SIN_INFORMACION);
        assertThat(log(salida)).contains("ERROR")
                .contains("No se pudieron leer los recuerdos para el chat (InvalidDataAccessResourceUsageException)")
                .doesNotContain("cancelada por el tope").doesNotContain(MENSAJE_SECRETO);
    }

    @Test
    void sinNombresConocidosNoHayErrorNiCancelacion(CapturedOutput salida) {
        when(lectura.estadoDeTodas(PERSONA)).thenReturn(UNA_RESPONDIDA);
        when(lectura.personasConocidas(PERSONA)).thenReturn(List.<NombreConocido>of());

        RecuerdosParaElChat.Resultado resultado = servicio(5000).armar(CUENTA, "Hoy me acordé de Elena");

        assertThat(resultado.progreso()).startsWith("Respondió");
        assertThat(log(salida)).doesNotContain("ERROR").doesNotContain("cancelada");
    }
}
