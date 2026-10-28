import { useState } from "react"

import { anuncioDeshecha, anuncioTomada } from "./mensajes"
import type { EstadoNotificacion, ItemAgenda } from "./tipos"
import { useCambiarEstado } from "./useRecordatorios"

/**
 * Marcar una toma como tomada o deshacerla, desde la Lista o desde el detalle de un día del
 * Calendario. Además del cambio, deja el aviso para el lector de pantalla (`anuncio`), el
 * error si falló y el id del aviso que se está guardando (para deshabilitar su botón).
 */
export function useCambioDeEstado() {
  const cambiarEstado = useCambiarEstado()
  const [anuncio, setAnuncio] = useState("")
  const [error, setError] = useState<unknown>(null)

  function cambiar(item: ItemAgenda, estado: EstadoNotificacion) {
    const nombre = item.recordatorio.tipo === "MEDICACION" ? item.recordatorio.nombreMedicamento : ""
    setError(null)
    cambiarEstado.mutate(
      { notificacionId: item.notificacionId, estado },
      {
        onSuccess: () => setAnuncio(estado === "TOMADA" ? anuncioTomada(nombre) : anuncioDeshecha(nombre)),
        onError: (falla) => setError(falla),
      }
    )
  }

  const ocupadoId = cambiarEstado.isPending ? cambiarEstado.variables?.notificacionId : undefined

  return { cambiar, ocupadoId, anuncio, error }
}
