import { HugeiconsIcon } from "@hugeicons/react"

import { Badge } from "@/components/ui/badge"
import {
  ESTILO_CATEGORIA_MICROCAPSULA,
  TEXTO_CATEGORIA_MICROCAPSULA,
  type CategoriaMicrocapsula,
} from "@/features/admin/etiquetas"

/** Etiqueta de categoría de microcápsula (Figma 51:1740): fondo pastel y, en algunas, ícono. */
export function EtiquetaCategoriaMicrocapsula({ categoria }: { categoria: CategoriaMicrocapsula }) {
  const estilo = ESTILO_CATEGORIA_MICROCAPSULA[categoria]
  return (
    <Badge className={estilo.fondo}>
      {estilo.icono ? <HugeiconsIcon icon={estilo.icono} aria-hidden="true" /> : null}
      {TEXTO_CATEGORIA_MICROCAPSULA[categoria]}
    </Badge>
  )
}
