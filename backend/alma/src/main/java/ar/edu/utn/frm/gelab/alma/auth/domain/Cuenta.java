package ar.edu.utn.frm.gelab.alma.auth.domain;

import java.time.LocalDate;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuenta de acceso (DC: Cuenta). Una sola tabla para personas mayores y administradores.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombreUsuario;

    /** Hash BCrypt, nunca la contraseña en texto plano. */
    @Column(nullable = false)
    private String contrasenia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private LocalDate fechaAlta;

    /** El nombre de usuario se guarda sin espacios a los costados y en minúsculas. */
    @PrePersist
    @PreUpdate
    void normalizarNombreUsuario() {
        if (nombreUsuario != null) {
            nombreUsuario = nombreUsuario.trim().toLowerCase(Locale.ROOT);
        }
    }
}
