package ar.edu.utn.frm.gelab.alma.ia;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Atiende los pedidos al modelo de a uno (el plan gratuito admite un solo pedido simultáneo). Quien no
 * consigue turno en el tiempo de espera falla con {@link ModeloNoDisponibleException}. El turno se libera
 * en un {@code finally}, cuando la llamada termina de verdad, también si falla.
 */
public class LimitadorDePedidos {

    private final Semaphore turno = new Semaphore(1, true);
    private final Duration esperaMaxima;

    public LimitadorDePedidos(Duration esperaMaxima) {
        this.esperaMaxima = esperaMaxima;
    }

    public <T> T ejecutar(Supplier<T> llamada) {
        boolean conseguido;
        try {
            conseguido = turno.tryAcquire(esperaMaxima.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModeloNoDisponibleException("Interrumpido mientras esperaba turno", e);
        }
        if (!conseguido) {
            throw new ModeloNoDisponibleException("No se consiguió turno para llamar al modelo");
        }
        try {
            return llamada.get();
        } finally {
            turno.release();
        }
    }
}
