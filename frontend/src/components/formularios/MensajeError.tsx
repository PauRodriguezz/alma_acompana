import type * as React from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { AlertCircleIcon } from "@hugeicons/core-free-icons"

/**
 * Mensaje de error junto a un campo: ícono + texto en destructive (el color solo nunca
 * alcanza). Es el mismo que dibuja CampoTexto; el campo lo enlaza con aria-describedby.
 */
export function MensajeError({ id, children }: { id?: string; children: React.ReactNode }) {
  return (
    <p id={id} className="flex items-start gap-2 text-destructive">
      <HugeiconsIcon icon={AlertCircleIcon} className="mt-0.5 size-6 shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </p>
  )
}
