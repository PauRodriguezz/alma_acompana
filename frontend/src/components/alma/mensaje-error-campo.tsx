import type { ReactNode } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { AlertCircleIcon } from "@hugeicons/core-free-icons"

/** Mensaje de error de un campo: ícono y texto (el color solo nunca alcanza), como en CampoTexto. */
export function MensajeErrorCampo({ id, children }: { id: string; children: ReactNode }) {
  return (
    <p id={id} className="flex items-start gap-2 text-destructive">
      <HugeiconsIcon icon={AlertCircleIcon} className="mt-0.5 size-6 shrink-0" aria-hidden="true" />
      <span>{children}</span>
    </p>
  )
}
