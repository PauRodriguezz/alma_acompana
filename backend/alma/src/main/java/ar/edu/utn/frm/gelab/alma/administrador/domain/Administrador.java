package ar.edu.utn.frm.gelab.alma.administrador.domain;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
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
 * Administrador (operador) de Alma (DC: Administrador).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Administrador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @OneToOne(optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    private Cuenta cuenta;
}
