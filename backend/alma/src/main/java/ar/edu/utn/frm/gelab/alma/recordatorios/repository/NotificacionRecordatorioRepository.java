package ar.edu.utn.frm.gelab.alma.recordatorios.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.NotificacionRecordatorio;
import ar.edu.utn.frm.gelab.alma.recordatorios.domain.Recordatorio;

/**
 * Las consultas de lectura llevan la cuenta del token. Los borrados masivos reciben ids que el
 * servicio ya verificó como de la persona.
 */
public interface NotificacionRecordatorioRepository extends JpaRepository<NotificacionRecordatorio, Long> {

    /** Notificaciones de [desde, hasta) de la persona, solo de las clases de recordatorio indicadas. */
    @Query("""
            select n from NotificacionRecordatorio n join fetch n.recordatorio r
            where r.personaMayor.cuenta.id = :cuentaId
              and n.fechaHoraProgramada >= :desde and n.fechaHoraProgramada < :hasta
              and type(r) in :clases
            order by n.fechaHoraProgramada, n.id
            """)
    List<NotificacionRecordatorio> agenda(@Param("cuentaId") Long cuentaId,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
            @Param("clases") Collection<Class<? extends Recordatorio>> clases);

    Optional<NotificacionRecordatorio> findByIdAndRecordatorioPersonaMayorCuentaId(Long id, Long cuentaId);

    List<NotificacionRecordatorio> findByRecordatorioIdAndRecordatorioPersonaMayorCuentaId(
            Long recordatorioId, Long cuentaId);

    /** Las notificaciones de varios recordatorios de la persona, en una sola consulta. */
    List<NotificacionRecordatorio> findByRecordatorioIdInAndRecordatorioPersonaMayorCuentaId(
            Collection<Long> recordatorioIds, Long cuentaId);

    /**
     * Borra las notificaciones de un recordatorio. Vuelca los cambios pendientes antes de borrar y
     * limpia el contexto después: no se deben modificar entidades cargadas después de llamarlo.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NotificacionRecordatorio n where n.recordatorio.id = :recordatorioId")
    void borrarDeRecordatorio(@Param("recordatorioId") Long recordatorioId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NotificacionRecordatorio n where n.id in :ids")
    void borrarPorIds(@Param("ids") Collection<Long> ids);
}
