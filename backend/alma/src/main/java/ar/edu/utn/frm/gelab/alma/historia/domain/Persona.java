package ar.edu.utn.frm.gelab.alma.historia.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Persona mencionada en un recuerdo (nodo de solo lectura, ver {@link PersonaMayorNodo}). El nombre puede
 * faltar si solo se mencionó el vínculo ("mi tío"). El vínculo no está acá: es propiedad de la relación
 * {@code CONOCE}.
 */
@Node("Persona")
public class Persona {

    @Id
    private final String idPersona;
    private final String nombre;
    private final String nombreNormalizado;

    public Persona(String idPersona, String nombre, String nombreNormalizado) {
        this.idPersona = idPersona;
        this.nombre = nombre;
        this.nombreNormalizado = nombreNormalizado;
    }

    public String getIdPersona() {
        return idPersona;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreNormalizado() {
        return nombreNormalizado;
    }
}
