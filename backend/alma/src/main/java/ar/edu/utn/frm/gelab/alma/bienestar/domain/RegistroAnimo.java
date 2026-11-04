package ar.edu.utn.frm.gelab.alma.bienestar.domain;

import java.time.LocalDateTime;

import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Ánimo que eligió la persona en un momento (DC: RegistroAnimo). Nunca se borra. La fecha y hora
 * las pone el back con el Clock de la app. El índice acelera la consulta del ánimo de hoy.
 */
@Entity
@Table(indexes = @Index(name = "ix_registro_animo_persona_fecha", columnList = "persona_mayor_id, fecha_hora"))
@Getter
@Setter
@NoArgsConstructor
public class RegistroAnimo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelAnimo nivelAnimo;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(optional = false)
    @JoinColumn(name = "persona_mayor_id", nullable = false)
    private PersonaMayor personaMayor;
}
