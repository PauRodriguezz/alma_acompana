package ar.edu.utn.frm.gelab.alma.persona.domain;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persona mayor usuaria de Alma (DC: PersonaMayor).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PersonaMayor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    private LocalDate fechaNacimiento;

    @OneToOne(optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    private Cuenta cuenta;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "preferencia_accesibilidad_id", nullable = false, unique = true)
    private PreferenciaAccesibilidad preferenciaAccesibilidad;
}
