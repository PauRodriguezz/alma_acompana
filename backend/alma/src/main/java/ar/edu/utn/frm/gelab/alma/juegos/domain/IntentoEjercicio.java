package ar.edu.utn.frm.gelab.alma.juegos.domain;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un ejercicio respondido (DC: IntentoEjercicio, CU-05). Un ejercicio abandonado no deja intento.
 * fechaHora lleva la hora (el DC dice Date): hace falta para la ronda y para "jugado hace más tiempo".
 */
@Entity
@Table(indexes = @Index(name = "idx_intento_persona_fecha", columnList = "persona_mayor_id, fecha_hora"))
@Getter
@Setter
@NoArgsConstructor
public class IntentoEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_mayor_id", nullable = false)
    private PersonaMayor personaMayor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;

    /** Fecha y hora de Argentina, sin zona. */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Boolean acierto;
}
