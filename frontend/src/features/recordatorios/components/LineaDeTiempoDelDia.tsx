import type { EstadoNotificacion, ItemAgenda } from "../tipos"
import { NOMBRE_LINEA_DE_TIEMPO } from "../mensajes"

import { TarjetaAviso } from "./Tarjetas"

type Props = {
  /** Los avisos del día. */
  items: ItemAgenda[]
  ahora: Date
  /** Id del aviso que se está guardando, para deshabilitar su botón. */
  ocupadoId?: number
  /** Solo para el día de hoy: sin esto, las medicaciones muestran su estado y nada más. */
  onCambiarEstado?: (item: ItemAgenda, estado: EstadoNotificacion) => void
}

/** Agrupa por franja ("HH:mm") y ordena por hora: lo que cae a la misma hora va junto (5.7). */
function porHorario(items: ItemAgenda[]): [string, ItemAgenda[]][] {
  const franjas = new Map<string, ItemAgenda[]>()
  for (const item of items) {
    const hora = item.fechaHoraProgramada.slice(11, 16)
    franjas.set(hora, [...(franjas.get(hora) ?? []), item])
  }
  return [...franjas.entries()].sort(([a], [b]) => a.localeCompare(b))
}

/**
 * Detalle del día en línea de tiempo vertical: un nodo por horario, la hora arriba y grande, y
 * debajo las tarjetas de esa hora. La línea y los nodos son decorativos.
 */
export function LineaDeTiempoDelDia({ items, ahora, ocupadoId, onCambiarEstado }: Props) {
  return (
    <ol aria-label={NOMBRE_LINEA_DE_TIEMPO} className="flex flex-col">
      {porHorario(items).map(([hora, delHorario]) => (
        <li
          key={hora}
          className="relative pb-6 pl-10 last:pb-0 before:absolute before:top-3 before:-bottom-1 before:left-[calc(0.75rem-1px)] before:w-0.5 before:bg-border last:before:hidden contraste-alto:before:bg-foreground"
        >
          <span
            aria-hidden="true"
            className="absolute top-1 left-0 size-6 rounded-full border-4 border-accent bg-card contraste-alto:border-foreground"
          />
          <h3 className="font-heading text-h2 font-medium text-heading">
            <time dateTime={hora}>{hora}</time>
          </h3>
          <ul className="mt-2 flex flex-col gap-3">
            {delHorario.map((item) => (
              <li key={item.notificacionId}>
                <TarjetaAviso
                  item={item}
                  ahora={ahora}
                  mostrarHora={false}
                  ocupado={ocupadoId === item.notificacionId}
                  onCambiarEstado={
                    onCambiarEstado ? (estado) => onCambiarEstado(item, estado) : undefined
                  }
                />
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ol>
  )
}
