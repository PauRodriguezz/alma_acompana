package ar.edu.utn.frm.gelab.alma.recordatorios.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recordatorio de medicación (DC: RecordatorioMedicacion).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RecordatorioMedicacion extends Recordatorio {

    @Column(nullable = false, length = 100)
    private String nombreMedicamento;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(nullable = false)
    private LocalTime horaToma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrecuenciaToma frecuencia;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private int duracionDias;
}
