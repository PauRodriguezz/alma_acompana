import { HugeiconsIcon } from "@hugeicons/react"
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

/**
 * "‹ Texto" arriba de una pantalla para volver a la anterior (Figma). Zona de al menos 44 px.
 * PENDIENTE: unificar con el EnlaceVolver de recordatorios (features/recordatorios/components),
 * que es igual pero con el destino fijo, en una tarea aparte.
 */
export function EnlaceVolver({ to, texto }: { to: string; texto: string }) {
  return (
    <Link
      to={to}
      className="inline-flex min-h-[calc(44rem/17)] items-center gap-1 self-start font-heading text-button font-medium text-heading"
    >
      <HugeiconsIcon icon={ArrowLeft01Icon} className="size-6 shrink-0" aria-hidden="true" />
      {texto}
    </Link>
  )
}
