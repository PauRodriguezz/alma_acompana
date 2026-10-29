import { useEffect, useRef } from "react"

import { Alert } from "@/components/ui/alert"

import type { Aviso } from "./aviso"

/**
 * Aviso de éxito (contrato del panel, sección 6): Alert "exito" con role="status". Al
 * aparecer recibe el foco, así el lector de pantalla lo lee aunque la pantalla sea nueva
 * (al volver del formulario) o la fila que tenía el foco haya desaparecido (después de una baja).
 */
export function AvisoExito({ aviso }: { aviso: Aviso | null }) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (aviso) ref.current?.focus()
  }, [aviso])

  if (!aviso) return null
  return (
    <Alert ref={ref} tabIndex={-1} variant="exito">
      {aviso.texto}
    </Alert>
  )
}
