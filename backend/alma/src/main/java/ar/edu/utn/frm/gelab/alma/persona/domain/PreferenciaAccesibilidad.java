package ar.edu.utn.frm.gelab.alma.persona.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Preferencias de accesibilidad de una persona mayor (DC: PreferenciaAccesibilidad).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PreferenciaAccesibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TamanioLetra tamanioLetra;

    @Column(nullable = false)
    private Boolean altoContraste;
}
