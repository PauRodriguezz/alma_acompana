import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { Brain02Icon, HashtagIcon, Link01Icon, QuoteDownIcon } from "@hugeicons/core-free-icons"

import { Chip } from "@/components/alma/chip"
import {
  ESTILO_CATEGORIA_EJERCICIO,
  TEXTO_CATEGORIA_EJERCICIO,
} from "@/features/admin/etiquetas"

import type { CategoriaEjercicio } from "../tipos"

// El Figma (134:5178) solo dibuja Refranes (comillas). PROPUESTOS: los íconos de las otras tres.
const ICONO: Record<CategoriaEjercicio, IconSvgElement> = {
  REFRAN: QuoteDownIcon,
  SERIE: HashtagIcon,
  ASOCIACION: Link01Icon,
  MEMORIA: Brain02Icon,
}

/** Chip de categoría del ejercicio: fondo game-*-soft y el ícono sobre un círculo del color del juego. */
export function ChipCategoria({ categoria }: { categoria: CategoriaEjercicio }) {
  const estilo = ESTILO_CATEGORIA_EJERCICIO[categoria]
  return (
    <Chip className={`gap-2 self-start py-1 pl-1.5 text-label ${estilo.fondo}`}>
      <span
        aria-hidden="true"
        className={`inline-flex size-[calc(26rem/17)] shrink-0 items-center justify-center rounded-full text-foreground ${estilo.punto}`}
      >
        <HugeiconsIcon icon={ICONO[categoria]} className="size-[calc(16rem/17)]" />
      </span>
      {TEXTO_CATEGORIA_EJERCICIO[categoria]}
    </Chip>
  )
}
