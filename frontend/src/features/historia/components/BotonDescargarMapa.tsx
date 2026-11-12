import { useState } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Download01Icon } from "@hugeicons/core-free-icons"

import { useAuth } from "@/features/auth/useAuth"
import { Button } from "@/components/ui/button"

import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import { descargarMapa } from "../descarga/descargarMapa"
import { DESCARGAR, DESCARGAR_MI_MAPA, ERROR_AL_CREAR_LA_IMAGEN } from "../mensajes"

/**
 * "Descargar" (contrato del mapa de vida, 2.5): crea la imagen en el dispositivo y la comparte o la baja. Si falla,
 * avisa con `onError` (la pantalla muestra el mensaje). Mientras crea la imagen queda con `aria-disabled`, así un
 * doble toque no la genera dos veces.
 */
export function BotonDescargarMapa({
  grupos,
  onError,
}: {
  grupos: EtapaConRecuerdos[]
  onError: (mensaje: string | null) => void
}) {
  const { usuario } = useAuth()
  const [creando, setCreando] = useState(false)

  async function alTocar() {
    if (creando) return
    setCreando(true)
    onError(null)
    try {
      await descargarMapa(grupos, usuario?.nombre)
    } catch {
      // Sin detalles técnicos para la persona; el log no lleva datos suyos.
      onError(ERROR_AL_CREAR_LA_IMAGEN)
    } finally {
      setCreando(false)
    }
  }

  return (
    <Button
      type="button"
      variant="outline"
      aria-label={DESCARGAR_MI_MAPA}
      aria-disabled={creando}
      aria-busy={creando}
      onClick={alTocar}
      className="shrink-0 aria-disabled:pointer-events-none"
    >
      <HugeiconsIcon icon={Download01Icon} className="size-5" aria-hidden="true" />
      {DESCARGAR}
    </Button>
  )
}
