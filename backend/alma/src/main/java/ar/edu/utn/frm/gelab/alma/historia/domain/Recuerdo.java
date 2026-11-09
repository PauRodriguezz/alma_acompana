package ar.edu.utn.frm.gelab.alma.historia.domain;

import java.time.ZonedDateTime;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Recuerdo de una persona (nodo de solo lectura, ver {@link PersonaMayorNodo}). El id es un UUID generado por
 * la aplicación. {@code anio}, {@code precision} y {@code etapaDeVida} son nulos si el recuerdo quedó sin año
 * (regla de integridad 6). Los tres contadores de intentos valen 0 si no existen.
 */
@Node("Recuerdo")
public class Recuerdo {

    @Id
    private final String idRecuerdo;
    private final String titulo;
    private final String descripcion;
    private final Integer anio;
    private final PrecisionAnio precision;
    private final EtapaDeVida etapaDeVida;
    private final ZonedDateTime fechaRegistro;
    private final Integer intentosAnio;
    private final Integer intentosLugares;
    private final Integer intentosPersonas;

    public Recuerdo(String idRecuerdo, String titulo, String descripcion, Integer anio, PrecisionAnio precision,
            EtapaDeVida etapaDeVida, ZonedDateTime fechaRegistro, Integer intentosAnio, Integer intentosLugares,
            Integer intentosPersonas) {
        this.idRecuerdo = idRecuerdo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.anio = anio;
        this.precision = precision;
        this.etapaDeVida = etapaDeVida;
        this.fechaRegistro = fechaRegistro;
        this.intentosAnio = intentosAnio;
        this.intentosLugares = intentosLugares;
        this.intentosPersonas = intentosPersonas;
    }

    public String getIdRecuerdo() {
        return idRecuerdo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getAnio() {
        return anio;
    }

    public PrecisionAnio getPrecision() {
        return precision;
    }

    public EtapaDeVida getEtapaDeVida() {
        return etapaDeVida;
    }

    public ZonedDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public Integer getIntentosAnio() {
        return intentosAnio;
    }

    public Integer getIntentosLugares() {
        return intentosLugares;
    }

    public Integer getIntentosPersonas() {
        return intentosPersonas;
    }
}
