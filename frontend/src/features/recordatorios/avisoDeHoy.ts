import { horaDeFechaHora } from "./fechas"
import type { ItemAgenda } from "./tipos"

// Lo usa la tarjeta "Para hoy", que también monta el Chat en su saludo (CU-02).

/** Un aviso de la tarjeta "Para hoy": todos los recordatorios pendientes de la misma franja horaria. */
export type AvisoDeHoy = {
  /** Identifica al aviso: cambia cuando cambia la franja o sus recordatorios. */
  clave: string
  /** "09:00" */
  hora: string
  items: ItemAgenda[]
}

/**
 * Regla 5.7 del contrato: el aviso es la primera notificación PENDIENTE del día, en orden de
 * hora (las atrasadas cuentan). Si hay varias a la misma hora y minuto, forman un solo aviso.
 * Sin pendientes no hay aviso.
 */
export function elegirAviso(agendaDeHoy: ItemAgenda[]): AvisoDeHoy | null {
  const pendientes = agendaDeHoy
    .filter((item) => item.estado === "PENDIENTE")
    .sort(
      (a, b) =>
        a.fechaHoraProgramada.localeCompare(b.fechaHoraProgramada) || a.notificacionId - b.notificacionId
    )
  if (pendientes.length === 0) return null

  // "yyyy-MM-ddTHH:mm": la misma franja a nivel hora y minuto
  const franja = pendientes[0].fechaHoraProgramada.slice(0, 16)
  const items = pendientes.filter((item) => item.fechaHoraProgramada.slice(0, 16) === franja)
  return {
    clave: items.map((item) => item.notificacionId).join("-"),
    hora: horaDeFechaHora(pendientes[0].fechaHoraProgramada),
    items,
  }
}
