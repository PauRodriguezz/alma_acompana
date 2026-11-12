package ar.edu.utn.frm.gelab.alma.ia;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Atiende los pedidos al modelo de a uno (el plan gratuito admite un solo pedido simultáneo), con dos niveles
 * de prioridad (contrato del grafo, sección 2, "Prioridad del chat"):
 * <ul>
 *   <li>{@link #ejecutar}: el chat. Espera su turno hasta el tiempo máximo y, si no lo consigue, falla con
 *       {@link ModeloNoDisponibleException}. Los chats se atienden por orden de llegada.</li>
 *   <li>{@link #ejecutarEnSegundoPlano}: la extracción de recuerdos. <b>No tiene tope de espera</b> y solo toma
 *       el turno si ningún chat está esperando. Una extracción que ya está llamando al modelo no se corta:
 *       cada llamada tiene su propio tope de tiempo en el cliente HTTP, que no incluye esta espera.</li>
 * </ul>
 * El turno se libera en un {@code finally}, cuando la llamada termina de verdad, también si falla.
 *
 * <p>Cómo se coordinan: el turno es un {@link Semaphore} justo. Los chats cuentan en {@code chatsEsperando}
 * mientras esperan turno. La extracción espera en un monitor a que el turno esté libre y no haya chats
 * esperando, y nunca se queda bloqueada dentro de la cola del semáforo (así un chat que llega después no queda
 * detrás de ella). Quien suma, resta o suelta el turno avisa por el monitor; además la extracción vuelve a
 * mirar cada {@value #ESPERA_DE_SEGURIDAD_MS} ms, como red de seguridad contra un aviso perdido.
 */
public class LimitadorDePedidos {

    /** Cada cuánto vuelve a mirar una extracción que espera, aunque nadie le haya avisado. */
    static final long ESPERA_DE_SEGURIDAD_MS = 250;

    private final Semaphore turno = new Semaphore(1, true);
    private final Duration esperaMaxima;

    /** Protege los dos contadores y sirve de señal. */
    private final Object senal = new Object();
    private int chatsEsperando;
    private int extraccionesEsperando;

    public LimitadorDePedidos(Duration esperaMaxima) {
        this.esperaMaxima = esperaMaxima;
    }

    /** El chat: espera su turno hasta {@code esperaMaxima}. */
    public <T> T ejecutar(Supplier<T> llamada) {
        boolean conseguido;
        sumarChatEsperando();
        try {
            conseguido = turno.tryAcquire(esperaMaxima.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("Interrumpido mientras esperaba turno", e);
        } finally {
            restarChatEsperando();
        }
        if (!conseguido) {
            throw new ModeloNoDisponibleException("No se consiguió turno para llamar al modelo");
        }
        try {
            return llamada.get();
        } finally {
            turno.release();
            avisar();
        }
    }

    /**
     * La extracción: espera sin tope a que el turno esté libre y ningún chat esté esperando. Si el hilo se
     * interrumpe mientras espera (por ejemplo, en el apagado), falla con {@link ModeloNoDisponibleException} y
     * deja la marca de interrupción.
     */
    public <T> T ejecutarEnSegundoPlano(Supplier<T> llamada) {
        synchronized (senal) {
            extraccionesEsperando++;
        }
        try {
            while (true) {
                esperarTurnoLibreSinChats();
                if (!turno.tryAcquire()) {
                    continue;
                }
                if (hayChatsEsperando()) {
                    // Apareció un chat entre que miramos y tomamos el turno: se lo devolvemos.
                    turno.release();
                    avisar();
                    continue;
                }
                break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("Interrumpido mientras esperaba turno", e);
        } finally {
            synchronized (senal) {
                extraccionesEsperando--;
            }
        }
        try {
            return llamada.get();
        } finally {
            turno.release();
            avisar();
        }
    }

    private void esperarTurnoLibreSinChats() throws InterruptedException {
        synchronized (senal) {
            while (chatsEsperando > 0 || turno.availablePermits() == 0) {
                senal.wait(ESPERA_DE_SEGURIDAD_MS);
            }
        }
    }

    private boolean hayChatsEsperando() {
        synchronized (senal) {
            return chatsEsperando > 0;
        }
    }

    private void sumarChatEsperando() {
        synchronized (senal) {
            chatsEsperando++;
        }
    }

    private void restarChatEsperando() {
        synchronized (senal) {
            chatsEsperando--;
            senal.notifyAll();
        }
    }

    private void avisar() {
        synchronized (senal) {
            senal.notifyAll();
        }
    }

    /** Cuántos chats están esperando turno ahora. Para las pruebas. */
    int chatsEsperando() {
        synchronized (senal) {
            return chatsEsperando;
        }
    }

    /** Cuántas extracciones están esperando turno ahora. Para las pruebas. */
    int extraccionesEsperando() {
        synchronized (senal) {
            return extraccionesEsperando;
        }
    }
}
