package ar.edu.utn.frm.gelab.alma.persona.dto;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;

/**
 * Datos personales de la persona del token (contrato, sección 4.4). La fecha de nacimiento puede ser null.
 */
public record PerfilDto(String nombre, String apellido, LocalDate fechaNacimiento, String nombreUsuario) {

    public static PerfilDto de(PersonaMayor persona) {
        return new PerfilDto(persona.getNombre(), persona.getApellido(), persona.getFechaNacimiento(),
                persona.getCuenta().getNombreUsuario());
    }
}
