import { EtiquetaCategoriaMicrocapsula } from "@/features/microcapsulas/EtiquetaCategoriaMicrocapsula"

import type { CategoriaMicrocapsula } from "../tipos"
import { MarcaGuardada } from "./MarcaGuardada"

/**
 * Etiqueta de categoría y, si está guardada, la marca "Guardada" (tarjeta y detalle).
 * El Badge de la etiqueta no parte el texto (whitespace-nowrap): con letra al 125 % o 150 % en
 * celular, "Prevención de caídas" se saldría de la tarjeta. Se lo deja partir desde acá
 * ([&>span]:whitespace-normal) sin modificar el componente del panel. Si aun así una palabra es
 * más larga que la tarjeta ("Recomendaciones" a 360 px con letra "Muy grande"), el chip no pasa del
 * ancho de la tarjeta (max-w-full) y corta la palabra (wrap-anywhere: solo `anywhere` baja el ancho
 * mínimo del chip; con `break-word` la grilla de la lista igual crece hasta el ancho de la palabra).
 */
export function EtiquetasLectura({
  categoria,
  guardada,
}: {
  categoria: CategoriaMicrocapsula
  guardada: boolean
}) {
  return (
    <div className="flex flex-wrap items-center gap-2 [&>span]:max-w-full [&>span]:wrap-anywhere [&>span]:whitespace-normal">
      <EtiquetaCategoriaMicrocapsula categoria={categoria} />
      {guardada ? <MarcaGuardada /> : null}
    </div>
  )
}
