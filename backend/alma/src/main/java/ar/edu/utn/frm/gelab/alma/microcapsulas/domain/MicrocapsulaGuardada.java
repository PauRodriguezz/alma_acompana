package ar.edu.utn.frm.gelab.alma.microcapsulas.domain;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Microcápsula que una persona mayor guardó para releer (DC: MicrocapsulaGuardada, CU-06).
 * La clave propia y la restricción única sobre (persona, microcápsula) son técnicas (contrato de
 * microcápsulas, sección 2). La baja lógica de la microcápsula no toca esta marca.
 */
@Entity
@Table(name = "microcapsula_guardada", uniqueConstraints = @UniqueConstraint(
        name = "uk_guardada_persona_microcapsula", columnNames = {"persona_mayor_id", "microcapsula_id"}))
@Getter
@Setter
@NoArgsConstructor
public class MicrocapsulaGuardada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_mayor_id", nullable = false)
    private PersonaMayor personaMayor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "microcapsula_id", nullable = false)
    private Microcapsula microcapsula;

    /** Fecha y hora de Argentina, sin zona. */
    @Column(name = "fecha_guardado", nullable = false)
    private LocalDateTime fechaGuardado;
}
