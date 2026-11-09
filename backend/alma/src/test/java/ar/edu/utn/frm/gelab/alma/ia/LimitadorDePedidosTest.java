package ar.edu.utn.frm.gelab.alma.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * El limitador con sus dos niveles (chat y segundo plano). Ninguna prueba espera sin tope: la clase tiene su
 * {@code @Timeout} y las esperas por un estado ({@link #esperarHasta}) tienen el suyo.
 */
@Timeout(value = 20, unit = TimeUnit.SECONDS)
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

    // ---------- Segundo plano (extracción) y prioridad del chat ----------

    @Test
    void sinChatsLaExtraccionTomaElTurnoDeInmediatoYDevuelveSuValor() {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));

        assertEquals("extraido", limitador.ejecutarEnSegundoPlano(() -> "extraido"));
        assertEquals(0, limitador.extraccionesEsperando());
    }

    @Test
    void conUnChatEnCursoLaExtraccionEsperaYCorreCuandoElChatTermina() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        List<String> orden = new CopyOnWriteArrayList<>();
        CountDownLatch chatEmpezo = new CountDownLatch(1);
        CountDownLatch soltarChat = new CountDownLatch(1);
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            Future<?> chat = hilos.submit(() -> limitador.ejecutar(() -> {
                chatEmpezo.countDown();
                esperar(soltarChat);
                orden.add("chat");
                return null;
            }));
            assertTrue(chatEmpezo.await(5, TimeUnit.SECONDS));
            Future<?> extraccion = hilos.submit(() -> limitador.ejecutarEnSegundoPlano(() -> orden.add("extraccion")));
            esperarHasta(() -> limitador.extraccionesEsperando() == 1);
            assertTrue(orden.isEmpty(), "La extracción no corre mientras el chat tiene el turno");

            soltarChat.countDown();
            chat.get(5, TimeUnit.SECONDS);
            extraccion.get(5, TimeUnit.SECONDS);

            assertEquals(List.of("chat", "extraccion"), orden);
        } finally {
            soltarChat.countDown();
            hilos.shutdownNow();
        }
    }

    /**
     * Se repite en rondas: sin la prioridad, el orden correcto depende de una carrera de microsegundos (el
     * semáforo justo le da el turno al chat casi siempre), así que una sola ronda no alcanza para detectar
     * una prioridad rota. Con la prioridad, todas las rondas dan el mismo orden.
     */
    @Test
    void unChatQueLlegaDespuesPasaAntesQueUnaExtraccionQueYaEstabaEsperando() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            chatExtraccionYOtroChat(ronda);
        }
    }

    private static final int RONDAS = 150;

    /** La extracción ya está registrada como esperando antes de que llegue el chat 2, y el chat 2 antes de soltar el 1. */
    private void chatExtraccionYOtroChat(int ronda) throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        List<String> orden = new CopyOnWriteArrayList<>();
        CountDownLatch chat1Empezo = new CountDownLatch(1);
        CountDownLatch soltarChat1 = new CountDownLatch(1);
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            Future<?> chat1 = hilos.submit(() -> limitador.ejecutar(() -> {
                chat1Empezo.countDown();
                esperar(soltarChat1);
                orden.add("chat1");
                return null;
            }));
            assertTrue(chat1Empezo.await(5, TimeUnit.SECONDS));
            Future<?> extraccion = hilos.submit(() -> limitador.ejecutarEnSegundoPlano(() -> orden.add("extraccion")));
            esperarHasta(() -> limitador.extraccionesEsperando() == 1);
            Future<?> chat2 = hilos.submit(() -> limitador.ejecutar(() -> orden.add("chat2")));
            esperarHasta(() -> limitador.chatsEsperando() == 1);

            soltarChat1.countDown();
            chat1.get(5, TimeUnit.SECONDS);
            chat2.get(5, TimeUnit.SECONDS);
            extraccion.get(5, TimeUnit.SECONDS);

            assertEquals(List.of("chat1", "chat2", "extraccion"), orden, "ronda " + ronda);
        } finally {
            soltarChat1.countDown();
            hilos.shutdownNow();
        }
    }

    @Test
    void conVariosChatsEsperandoLaExtraccionCorreRecienCuandoNoQuedaNinguno() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            dosChatsYUnaExtraccion(ronda);
        }
    }

    /** El chat A tiene el turno; la extracción, B y C (ya registrados como esperando) vienen detrás. */
    private void dosChatsYUnaExtraccion(int ronda) throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        List<String> orden = new CopyOnWriteArrayList<>();
        CountDownLatch chatAEmpezo = new CountDownLatch(1);
        CountDownLatch soltarChatA = new CountDownLatch(1);
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            Future<?> chatA = hilos.submit(() -> limitador.ejecutar(() -> {
                chatAEmpezo.countDown();
                esperar(soltarChatA);
                orden.add("A");
                return null;
            }));
            assertTrue(chatAEmpezo.await(5, TimeUnit.SECONDS));
            Future<?> extraccion = hilos.submit(() -> limitador.ejecutarEnSegundoPlano(() -> orden.add("extraccion")));
            esperarHasta(() -> limitador.extraccionesEsperando() == 1);
            Future<?> chatB = hilos.submit(() -> limitador.ejecutar(() -> {
                dormir(2);
                orden.add("B");
                return null;
            }));
            esperarHasta(() -> limitador.chatsEsperando() == 1);
            Future<?> chatC = hilos.submit(() -> limitador.ejecutar(() -> orden.add("C")));
            esperarHasta(() -> limitador.chatsEsperando() == 2);

            soltarChatA.countDown();
            for (Future<?> f : List.of(chatA, chatB, chatC, extraccion)) {
                f.get(5, TimeUnit.SECONDS);
            }

            assertEquals(List.of("A", "B", "C", "extraccion"), orden, "ronda " + ronda);
        } finally {
            soltarChatA.countDown();
            hilos.shutdownNow();
        }
    }

    @Test
    void laExtraccionEsperaMasQueElTopeDelChatYCorreSinError() throws Exception {
        // El tope de 100 ms es del chat: la extracción espera ~500 ms y no falla.
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofMillis(100));
        CountDownLatch chatEmpezo = new CountDownLatch(1);
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            Future<?> chat = hilos.submit(() -> limitador.ejecutar(() -> {
                chatEmpezo.countDown();
                dormir(500);
                return null;
            }));
            assertTrue(chatEmpezo.await(5, TimeUnit.SECONDS));
            long inicio = System.nanoTime();

            assertEquals("extraido", limitador.ejecutarEnSegundoPlano(() -> "extraido"));
            long esperadoMs = (System.nanoTime() - inicio) / 1_000_000;

            assertTrue(esperadoMs >= 300, "Esperó el turno, no falló a los 100 ms: " + esperadoMs + " ms");
            chat.get(5, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }
    }

    @Test
    void unChatQueSeRindeNoDejaElContadorSubidoYLaExtraccionCorreDespues() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofMillis(150));
        CountDownLatch chatEmpezo = new CountDownLatch(1);
        CountDownLatch soltarChat = new CountDownLatch(1);
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            Future<?> chat = hilos.submit(() -> limitador.ejecutar(() -> {
                chatEmpezo.countDown();
                esperar(soltarChat);
                return null;
            }));
            assertTrue(chatEmpezo.await(5, TimeUnit.SECONDS));
            Future<String> extraccion = hilos.submit(() -> limitador.ejecutarEnSegundoPlano(() -> "extraido"));
            esperarHasta(() -> limitador.extraccionesEsperando() == 1);

            // Este chat espera 150 ms y se rinde.
            assertThrows(ModeloNoDisponibleException.class, () -> limitador.ejecutar(() -> "segundo chat"));
            assertEquals(0, limitador.chatsEsperando());

            soltarChat.countDown();
            chat.get(5, TimeUnit.SECONDS);
            assertEquals("extraido", extraccion.get(5, TimeUnit.SECONDS));
        } finally {
            soltarChat.countDown();
            hilos.shutdownNow();
        }
    }

    @Test
    void unaExtraccionInterrumpidaMientrasEsperaFallaYConservaLaMarcaDeInterrupcion() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        CountDownLatch chatEmpezo = new CountDownLatch(1);
        CountDownLatch soltarChat = new CountDownLatch(1);
        AtomicBoolean fallo = new AtomicBoolean();
        AtomicBoolean marca = new AtomicBoolean();
        AtomicBoolean corrio = new AtomicBoolean();
        ExecutorService hilos = Executors.newCachedThreadPool();
        try {
            hilos.submit(() -> limitador.ejecutar(() -> {
                chatEmpezo.countDown();
                esperar(soltarChat);
                return null;
            }));
            assertTrue(chatEmpezo.await(5, TimeUnit.SECONDS));
            Thread extraccion = new Thread(() -> {
                try {
                    limitador.ejecutarEnSegundoPlano(() -> corrio.getAndSet(true));
                } catch (ModeloNoDisponibleException e) {
                    fallo.set(true);
                    marca.set(Thread.currentThread().isInterrupted());
                }
            });
            extraccion.start();
            esperarHasta(() -> limitador.extraccionesEsperando() == 1);

            extraccion.interrupt();
            extraccion.join(5000);

            assertFalse(extraccion.isAlive(), "La extracción dejó de esperar");
            assertTrue(fallo.get());
            assertTrue(marca.get(), "Conserva la marca de interrupción");
            assertFalse(corrio.get());
            assertEquals(0, limitador.extraccionesEsperando());
        } finally {
            soltarChat.countDown();
            hilos.shutdownNow();
        }
    }

    @Test
    void laExtraccionLiberaElTurnoTambienSiLaLlamadaFalla() {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofMillis(100));

        assertThrows(IllegalStateException.class, () -> limitador.ejecutarEnSegundoPlano(() -> {
            throw new IllegalStateException("falla");
        }));

        assertEquals("chat", limitador.ejecutar(() -> "chat"));
        assertEquals("otra", limitador.ejecutarEnSegundoPlano(() -> "otra"));
    }

    @Test
    void dosExtraccionesNuncaCorrenALaVez() throws Exception {
        LimitadorDePedidos limitador = new LimitadorDePedidos(Duration.ofSeconds(5));
        AtomicInteger enCurso = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();
        ExecutorService hilos = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> trabajos = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                trabajos.add(hilos.submit(() -> limitador.ejecutarEnSegundoPlano(() -> {
                    maximo.accumulateAndGet(enCurso.incrementAndGet(), Math::max);
                    dormir(50);
                    enCurso.decrementAndGet();
                    return null;
                })));
            }
            for (Future<?> trabajo : trabajos) {
                trabajo.get(10, TimeUnit.SECONDS);
            }
            assertEquals(1, maximo.get());
        } finally {
            hilos.shutdownNow();
        }
    }

    /** Espera (con tope de 5 s) a que se cumpla una condición; falla si no se cumple a tiempo. */
    static void esperarHasta(BooleanSupplier condicion) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condicion.getAsBoolean()) {
            if (System.nanoTime() > limite) {
                throw new AssertionError("La condición no se cumplió en 5 s");
            }
            dormir(5);
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
