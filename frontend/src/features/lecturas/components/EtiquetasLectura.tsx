import { EtiquetaCategoriaMicrocapsula } from "@/features/microcapsulas/EtiquetaCategoriaMicrocapsula"

import type { CategoriaMicrocapsula } from "../tipos"
import { MarcaGuardada } from "./MarcaGuardada"

/**
 * Etiqueta de categoría y, si está guardada, la marca "Guardada" (tarjeta y detalle).
 * El Badge de la etiqueta no parte el texto (whitespace-nowrap): con letra al 125 % o 150 % en
 * celular, "Prevención de caídas" se saldría de la tarjeta. Se lo deja partir desde acá
 * ([&>span]:whitespace-normal) sin modificar el componente del panel.
 * PENDIENTE: mover la etiqueta de categoría a components/alma/ (hoy vive en features/microcapsulas).
 */
export function EtiquetasLectura({
  categoria,
  guardada,
}: {
  categoria: CategoriaMicrocapsula
  guardada: boolean
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 [&>span]:whitespace-normal">
      <EtiquetaCategoriaMicrocapsula categoria={categoria} />
      {guardada ? <MarcaGuardada /> : null}
    </div>
  )
}
