package ar.edu.utn.frm.gelab.alma.historia.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/** Lugar de un recuerdo (nodo de solo lectura, ver {@link PersonaMayorNodo}). */
@Node("Lugar")
public class Lugar {

    @Id
    private final String idLugar;
    private final String nombre;
    private final String nombreNormalizado;

    public Lugar(String idLugar, String nombre, String nombreNormalizado) {
        this.idLugar = idLugar;
        this.nombre = nombre;
        this.nombreNormalizado = nombreNormalizado;
    }

    public String getIdLugar() {
        return idLugar;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }
}
