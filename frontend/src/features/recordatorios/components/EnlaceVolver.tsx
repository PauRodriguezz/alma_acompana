import { HugeiconsIcon } from "@hugeicons/react"
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

import { VOLVER_A_RECORDATORIOS } from "../mensajes"

/** "‹ Recordatorios" arriba de los formularios y del detalle (Figma). Zona de al menos 44 px. */
export function EnlaceVolver() {
  return (
    <Link
      to="/recordatorios"
      className="inline-flex min-h-[calc(44rem/17)] items-center gap-1 self-start font-heading text-button font-medium text-heading"
    >
      <HugeiconsIcon icon={ArrowLeft01Icon} className="size-6" aria-hidden="true" />
      {VOLVER_A_RECORDATORIOS}
    </Link>
  )
}
