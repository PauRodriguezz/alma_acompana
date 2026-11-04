package ar.edu.utn.frm.gelab.alma.juegos;

/**
 * No hay ningún ejercicio activo para jugar en ningún nivel. JuegoController la convierte en un 404 con
 * {@link MensajesJuego#SIN_JUEGOS_DISPONIBLES} (el 404 general siempre dice "No encontramos lo que buscás.").
 */
public class SinJuegosDisponiblesException extends RuntimeException {

    public SinJuegosDisponiblesException() {
        super(MensajesJuego.SIN_JUEGOS_DISPONIBLES);
    }
}
