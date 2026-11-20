import { useEffect, useState } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Cancel01Icon, CheckmarkCircle02Icon } from "@hugeicons/core-free-icons"
import { cn } from "@/lib/utils"

import { alertVariants } from "@/components/ui/alert-variants"
import { Button } from "@/components/ui/button"
import { AVISO_CERRAR } from "@/lib/mensajes"

// Al menos 8 segundos (a una persona mayor le lleva más leer y decidir).
const DURACION_MS = 8000

type Props = {
  texto: string
  /** Se terminó el tiempo. */
  onTiempo: () => void
  /** La persona apretó "Cerrar". */
  onCerrar: () => void
}

/**
 * Marco del aviso de éxito flotante (lo monta AvisoProvider). Ícono y texto (el color solo no
 * alcanza), botón "Cerrar" de al menos 48 px y medidas en rem para que crezca con "Muy grande".
 * El tiempo se pausa mientras el mouse o el foco están encima y, al salir, vuelve a contar los
 * 8 segundos completos. No atrapa el foco ni lo mueve al aparecer. Si el botón no entra al
 * costado (celular con letra "Muy grande"), pasa debajo del texto.
 */
export function AvisoFlotante({ texto, onTiempo, onCerrar }: Props) {
  const [conMouse, setConMouse] = useState(false)
  const [conFoco, setConFoco] = useState(false)
  const pausado = conMouse || conFoco

  useEffect(() => {
    if (pausado) return
    const id = window.setTimeout(onTiempo, DURACION_MS)
    return () => window.clearTimeout(id)
  }, [pausado, onTiempo])

  return (
    <div
      onMouseEnter={() => setConMouse(true)}
      onMouseLeave={() => setConMouse(false)}
      onFocus={() => setConFoco(true)}
      onBlur={(evento) => {
        if (!evento.currentTarget.contains(evento.relatedTarget)) setConFoco(false)
      }}
      className={cn(
        alertVariants({ variant: "exito" }),
        "pointer-events-auto w-[min(100%,calc(480rem/17))] flex-wrap items-center [box-shadow:var(--sombra-tarjeta)]",
        "motion-safe:animate-in motion-safe:fade-in-0 motion-safe:slide-in-from-bottom-2"
      )}
    >
      <HugeiconsIcon icon={CheckmarkCircle02Icon} className="size-6 shrink-0 text-success" aria-hidden="true" />
      <p className="min-w-0 flex-1 basis-[calc(200rem/17)] font-semibold break-words">{texto}</p>
      <Button variant="outline" className="mb-0 w-full shrink-0 sm:w-auto" onClick={onCerrar}>
        <HugeiconsIcon icon={Cancel01Icon} className="size-5" aria-hidden="true" />
        {AVISO_CERRAR}
      </Button>
    </div>
  )
}
