package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.repository.EstadoDePregunta;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.NombreConocido;
import ar.edu.utn.frm.gelab.alma.historia.service.RecuerdosParaElChat;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * El presupuesto de tiempo de {@link RecuerdosParaElChat} sin base de datos: el tope de tiempo, el rechazo inmediato
 * con el ejecutor lleno y que una lectura cancelada deje de hacer consultas.
 *
 * <p>Reglas de estas pruebas: toda espera tiene un timeout; un latch que no se libera hace <b>fallar</b> la prueba
 * (se anota el error y {@link #verificarQueNingunLatchQuedoSinLiberar} lo reporta) y no la cuelga; y los márgenes son
 * holgados (un tope de 300 ms contra límites de varios segundos), así que no dependen de que la máquina sea rápida.
 * Las pruebas de cancelación no usan un tope corto: cancelan interrumpiendo al hilo que llamó.
 */
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class RecuerdosParaElChatTopeTest {

    private static final String SIN_INFORMACION = "No hay información.";
    private static final long ESPERA_MAXIMA_S = 20;
    private static final long PERSONA_COLGADA = 7L;
    private static final long PERSONA_LIBRE = 8L;
    private static final Long CUENTA_COLGADA = 1L;
    private static final Long CUENTA_LIBRE = 2L;
    /** Una pregunta respondida y dos pendientes: el progreso dice "Respondió 1, salteó 0, quedan 2.". */
    private static final List<EstadoDePregunta> UNA_RESPONDIDA = List.of(
            new EstadoDePregunta(1, 1, "Uno", "RESPONDIO", ZonedDateTime.now()),
            new EstadoDePregunta(2, 2, "Dos", null, null), new EstadoDePregunta(3, 3, "Tres", null, null));

    private final LecturaDelGrafo lectura = mock(LecturaDelGrafo.class);
    /** La lectura "colgada" empezó (se anota apenas entra a la consulta que se bloquea). */
    private final CountDownLatch consultaEmpezo = new CountDownLatch(1);
    /** Libera las lecturas bloqueadas. Si nadie lo libera ni interrumpe, la lectura anota un error y sigue. */
    private final CountDownLatch soltar = new CountDownLatch(1);
    private final List<Throwable> errores = new CopyOnWriteArrayList<>();

    @AfterEach
    void verificarQueNingunLatchQuedoSinLiberar() {
        soltar.countDown();
        assertThat(errores).as("una lectura bloqueada nadie la liberó ni la interrumpió").isEmpty();
    }

    // ---------- Armado ----------

    private RecuerdosParaElChat servicio(int hilos, long topeMs) {
        PersonaMayorRepository repositorio = mock(PersonaMayorRepository.class);
        // Los mocks se crean antes de los when(...): crear uno adentro de otro stubbing rompe Mockito.
        PersonaMayor colgada = persona(PERSONA_COLGADA);
        PersonaMayor libre = persona(PERSONA_LIBRE);
        when(repositorio.findByCuentaId(CUENTA_COLGADA)).thenReturn(Optional.of(colgada));
        when(repositorio.findByCuentaId(CUENTA_LIBRE)).thenReturn(Optional.of(libre));
        ConfiguracionHistoria configuracion = new ConfiguracionHistoria();
        configuracion.setLecturaChatHilosMax(hilos);
        configuracion.setLecturaChatTimeoutMs(topeMs);
        return new RecuerdosParaElChat(repositorio, lectura, configuracion);
    }

    private static PersonaMayor persona(long id) {
        PersonaMayor persona = mock(PersonaMayor.class);
        when(persona.getId()).thenReturn(id);
        return persona;
    }

    /**
     * Bloquea al hilo de la lectura hasta que lo suelten o lo interrumpan. Si lo interrumpen (la cancelación del
     * tope), restaura la marca de interrupción como haría un driver bien portado, y sigue. Si pasan 20 s sin
     * ninguna de las dos cosas, anota un error: la prueba falla en lugar de colgarse.
     */
    private void bloquear() {
        consultaEmpezo.countDown();
        try {
            if (!soltar.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS)) {
                errores.add(new AssertionError("La lectura bloqueada esperó " + ESPERA_MAXIMA_S
                        + " s sin que la soltaran ni la interrumpieran"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Answer<List<EstadoDePregunta>> progresoColgado() {
        return invocacion -> {
            bloquear();
            return UNA_RESPONDIDA;
        };
    }

    private static void esperar(CountDownLatch latch, String que) throws InterruptedException {
        assertThat(latch.await(ESPERA_MAXIMA_S, TimeUnit.SECONDS)).as(que).isTrue();
    }

    /**
     * Llama a {@code armar} en otro hilo, espera a que la lectura bloquee y entonces interrumpe a ese hilo llamador:
     * {@code armar} cancela la lectura (interrumpe a su hilo) y vuelve. Sin tope corto de por medio.
     */
    private void cancelarLaLecturaBloqueada(RecuerdosParaElChat servicio, String mensaje) throws InterruptedException {
        Thread llamador = new Thread(() -> servicio.armar(CUENTA_COLGADA, mensaje), "llamador-de-prueba");
        llamador.start();
        esperar(consultaEmpezo, "la lectura llegó a la consulta que se bloquea");
        llamador.interrupt();
        llamador.join(Duration.ofSeconds(ESPERA_MAXIMA_S));
        assertThat(llamador.isAlive()).as("armar volvió después de cancelar").isFalse();
    }

    /**
     * Intenta {@code armar} con otra persona hasta que lo acepten: con un solo hilo, que lo acepten prueba que la
     * lectura anterior terminó del todo (si no, se rechaza). Sin esperas fijas: se repite hasta 20 s.
     */
    private RecuerdosParaElChat.Resultado hastaQueSeAcepte(RecuerdosParaElChat servicio, Long cuenta) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_MAXIMA_S);
        while (System.nanoTime() < limite) {
            RecuerdosParaElChat.Resultado resultado = servicio.armar(cuenta, "Hola");
            if (resultado.progreso().startsWith("Respondió")) {
                return resultado;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
        }
        throw new AssertionError("El ejecutor siguió ocupado más de " + ESPERA_MAXIMA_S + " s");
    }

    // ---------- Tope de tiempo y rechazo ----------

    @Test
    void unaLecturaColgadaSeCortaAlLlegarAlTope() {
        when(lectura.estadoDeTodas(PERSONA_COLGADA)).thenAnswer(progresoColgado());
        RecuerdosParaElChat servicio = servicio(2, 300);

        long inicio = System.nanoTime();
        RecuerdosParaElChat.Resultado resultado = servicio.armar(CUENTA_COLGADA, "Hola Elena");
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(resultado.progreso()).isEqualTo(SIN_INFORMACION);
        assertThat(resultado.recuerdos()).isEqualTo(SIN_INFORMACION);
        // Tope 300 ms: vuelve después de él y mucho antes de los 20 s que tardaría la lectura sin tope.
        assertThat(tardo).isGreaterThanOrEqualTo(Duration.ofMillis(250)).isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void conElEjecutorLlenoSeRechazaDeInmediatoSinEsperarElTopeYSinLoguearElMensaje(CapturedOutput salida)
            throws Exception {
        when(lectura.estadoDeTodas(PERSONA_COLGADA)).thenAnswer(progresoColgado());
        // Un solo hilo y un tope largo (25 s): si la segunda llamada esperara, tardaría 25 s.
        RecuerdosParaElChat servicio = servicio(1, 25_000);

        CompletableFuture<RecuerdosParaElChat.Resultado> primera = CompletableFuture
                .supplyAsync(() -> servicio.armar(CUENTA_COLGADA, "Primera"));
        esperar(consultaEmpezo, "la primera lectura ocupó el único hilo");

        long inicio = System.nanoTime();
        RecuerdosParaElChat.Resultado segunda = servicio.armar(CUENTA_COLGADA, "frase-secreta-del-rechazo");
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(segunda.progreso()).isEqualTo(SIN_INFORMACION);
        assertThat(segunda.recuerdos()).isEqualTo(SIN_INFORMACION);
        assertThat(tardo).as("rechazo inmediato, no el tope de 25 s").isLessThan(Duration.ofSeconds(10));
        // Se avisó del rechazo en el log, sin el texto de la persona.
        assertThat(salida.getAll()).contains("rechazada").doesNotContain("frase-secreta-del-rechazo");

        soltar.countDown();
        assertThat(primera.get(ESPERA_MAXIMA_S, TimeUnit.SECONDS)).isNotNull();
    }

    @Test
    void unaVezLiberadoElHiloLasLecturasVuelvenAFuncionar() {
        // La primera lectura se cuelga; las siguientes devuelven una respuesta real.
        when(lectura.estadoDeTodas(PERSONA_COLGADA)).thenAnswer(progresoColgado()).thenReturn(UNA_RESPONDIDA);
        RecuerdosParaElChat servicio = servicio(1, 300);

        RecuerdosParaElChat.Resultado colgada = servicio.armar(CUENTA_COLGADA, "Se cuelga");
        assertThat(colgada.progreso()).isEqualTo(SIN_INFORMACION);

        // El tope la canceló: apenas el hilo queda libre, la lectura normal responde con datos reales.
        RecuerdosParaElChat.Resultado resultado = hastaQueSeAcepte(servicio, CUENTA_COLGADA);

        assertThat(resultado.progreso()).isEqualTo("Respondió 1, salteó 0, quedan 2.");
    }

    // ---------- Una lectura cancelada deja de hacer consultas ----------

    @Test
    void unaLecturaCanceladaEnElBucleDePersonasNoHaceMasConsultas() throws Exception {
        when(lectura.estadoDeTodas(anyLong())).thenReturn(UNA_RESPONDIDA);
        when(lectura.personasConocidas(anyLong())).thenReturn(List.of(new NombreConocido("Elena", "elena"),
                new NombreConocido("Rosa", "rosa"), new NombreConocido("Marta", "marta")));
        // La primera consulta de recuerdos por persona se bloquea; al cancelarla, devuelve con la marca puesta.
        when(lectura.recuerdosDePersona(anyLong(), anyString())).thenAnswer(invocacion -> {
            bloquear();
            return List.of();
        });
        RecuerdosParaElChat servicio = servicio(1, 25_000);

        cancelarLaLecturaBloqueada(servicio, "Elena, Rosa y Marta, de chica y de joven en la Escuela");
        hastaQueSeAcepte(servicio, CUENTA_LIBRE); // prueba que la lectura cancelada ya terminó

        // Solo se hizo la consulta que ya estaba en curso: ni Rosa ni Marta, ni lugares, ni etapas.
        verify(lectura, times(1)).recuerdosDePersona(eq(PERSONA_COLGADA), anyString());
        verify(lectura, never()).lugaresConocidos(PERSONA_COLGADA);
        verify(lectura, never()).recuerdosDeLugar(eq(PERSONA_COLGADA), anyString());
        verify(lectura, never()).recuerdosDeEtapa(eq(PERSONA_COLGADA), anyString());
    }

    @Test
    void unaLecturaCanceladaAntesDeLasEtapasNoLasConsulta() throws Exception {
        when(lectura.estadoDeTodas(anyLong())).thenReturn(UNA_RESPONDIDA);
        when(lectura.personasConocidas(anyLong())).thenReturn(List.of());
        // Los lugares se bloquean (consulta P17); al cancelar, vuelve con la marca puesta.
        when(lectura.lugaresConocidos(PERSONA_COLGADA)).thenAnswer(invocacion -> {
            bloquear();
            return List.of(new NombreConocido("la costa", "la costa"));
        });
        RecuerdosParaElChat servicio = servicio(1, 25_000);

        cancelarLaLecturaBloqueada(servicio, "Fuimos a la costa de chica");
        hastaQueSeAcepte(servicio, CUENTA_LIBRE);

        verify(lectura, never()).recuerdosDeLugar(eq(PERSONA_COLGADA), anyString());
        verify(lectura, never()).recuerdosDeEtapa(eq(PERSONA_COLGADA), anyString());
    }

    @Test
    void unaLecturaCanceladaTrasElProgresoNoLeeLosRecuerdos() throws Exception {
        // El progreso se bloquea; al cancelar, vuelve con la marca puesta y con datos válidos.
        when(lectura.estadoDeTodas(PERSONA_COLGADA)).thenAnswer(progresoColgado());
        when(lectura.estadoDeTodas(PERSONA_LIBRE)).thenReturn(UNA_RESPONDIDA);
        RecuerdosParaElChat servicio = servicio(1, 25_000);

        cancelarLaLecturaBloqueada(servicio, "Hola Elena de chica");
        hastaQueSeAcepte(servicio, CUENTA_LIBRE);

        verify(lectura, never()).personasConocidas(PERSONA_COLGADA);
        verify(lectura, never()).lugaresConocidos(PERSONA_COLGADA);
        verify(lectura, never()).recuerdosDeEtapa(eq(PERSONA_COLGADA), anyString());
    }
}
