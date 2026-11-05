package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class LimitadorDePedidosTest {

    @Test
    void devuelveLoQueDevuelveLaLlamada() {
        assertEquals("ok", new LimitadorDePedidos(Duration.ofMillis(100)).ejecutar(() -> "ok"));
    }

    @Test
    void liberaElTurnoTambienCuandoLaLlamadaFalla() {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofMillis(100));

        assertThrows(IllegalStateException.class, () -> limitador.ejecutar(() -> {
            throw new IllegalStateException("falla");
        }));

        // Si el turno no se hubiera liberado, esta llamada fallaría por no conseguir turno.
        assertEquals("ok", limitador.ejecutar(() -> "ok"));
    }

    @Test
    void unSegundoPedidoEsperaYFallaSiElTurnoNoSeLibera() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofMillis(200));
        CountDownLatch empezo = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        ExecutorService hilos = Executors.newSingleThreadExecutor();
        try {
            Future<String> primero = hilos.submit(() -> limitador.ejecutar(() -> {
                empezo.countDown();
                esperar(soltar);
                return "primero";
            }));
            assertEquals(true, empezo.await(2, TimeUnit.SECONDS));

            assertThrows(ModeloNoDisponibleException.class, () -> limitador.ejecutar(() -> "segundo"));

            soltar.countDown();
            assertEquals("primero", primero.get(2, TimeUnit.SECONDS));
            assertEquals("tercero", limitador.ejecutar(() -> "tercero"));
        } finally {
            soltar.countDown();
            hilos.shutdownNow();
        }
    }

    @Test
    void unSegundoPedidoQueEsperaPocoConsigueElTurnoCuandoSeLibera() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        CountDownLatch empezo = new CountDownLatch(1);
        ExecutorService hilos = Executors.newSingleThreadExecutor();
        try {
            Future<String> primero = hilos.submit(() -> limitador.ejecutar(() -> {
                empezo.countDown();
                dormir(300);
                return "primero";
            }));
            assertEquals(true, empezo.await(2, TimeUnit.SECONDS));

            assertEquals("segundo", limitador.ejecutar(() -> "segundo"));
            assertEquals("primero", primero.get(2, TimeUnit.SECONDS));
        } finally {
            hilos.shutdownNow();
        }
    }

    private static void esperar(CountDownLatch cerrojo) {
        try {
            cerrojo.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void dormir(long milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
