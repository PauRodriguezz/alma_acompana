package ar.edu.utn.frm.gelab.alma.recordatorios.domain;

import java.time.LocalDateTime;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Aviso programado de un recordatorio (DC: NotificacionRecordatorio). La clave única evita duplicar
 * una toma, y la restricción de base asegura que solo una notificación TOMADA tenga fechaHoraToma.
 * Que TOMADA sea de medicación y VISTA de cita lo controla el servicio.
 */
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = { "recordatorio_id", "fecha_hora_programada" }),
        check = @CheckConstraint(name = "ck_notificacion_toma_solo_tomada",
                constraint = "fecha_hora_toma is null or estado = 'TOMADA'"))
@Getter
@Setter
@NoArgsConstructor
public class NotificacionRecordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recordatorio_id", nullable = false)
    private Recordatorio recordatorio;

    @Column(nullable = false)
    private LocalDateTime fechaHoraProgramada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoNotificacion estado;

    private LocalDateTime fechaHoraToma;

    /** Notificación nueva, siempre PENDIENTE. */
    public NotificacionRecordatorio(Recordatorio recordatorio, LocalDateTime fechaHoraProgramada) {
        this.recordatorio = recordatorio;
        this.fechaHoraProgramada = fechaHoraProgramada;
        this.estado = EstadoNotificacion.PENDIENTE;
    }

    /** Si ya estaba TOMADA, conserva su fechaHoraToma original. */
    public void marcarTomada(LocalDateTime ahora) {
        if (estado != EstadoNotificacion.TOMADA) {
            estado = EstadoNotificacion.TOMADA;
            fechaHoraToma = ahora;
        }
    }

    public void marcarVista() {
        estado = EstadoNotificacion.VISTA;
        fechaHoraToma = null;
    }

    /** Vuelve a PENDIENTE (deshacer la toma o la vista) y borra la fechaHoraToma. */
    public void marcarPendiente() {
        estado = EstadoNotificacion.PENDIENTE;
        fechaHoraToma = null;
    }
}
