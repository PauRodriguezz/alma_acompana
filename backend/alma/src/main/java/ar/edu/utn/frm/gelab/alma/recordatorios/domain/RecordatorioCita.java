package ar.edu.utn.frm.gelab.alma.recordatorios.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recordatorio de cita médica (DC: RecordatorioCita). Es puntual: no se repite.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RecordatorioCita extends Recordatorio {

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, length = 100)
    private String especialidad;

    @Column(nullable = false, length = 100)
    private String medico;

    @Column(nullable = false, length = 150)
    private String lugar;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 500)
    private String indicacionesPrevias;
}
