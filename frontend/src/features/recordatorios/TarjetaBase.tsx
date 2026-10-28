import type { ReactNode } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  ArrowRight01Icon,
  MedicineBottle01Icon,
  Stethoscope02Icon,
} from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"
import { cn } from "cn"

import { Chip } from "@/components/alma/chip"

import { CHIP_CITA, CHIP_MEDICACION } from "./mensajes"
import type { TipoRecordatorio } from "./tipos"

// Cada tipo se distingue por ícono, etiqueta de texto y acento lateral (nunca solo color).
const TIPOS = {
  MEDICACION: {
    icono: MedicineBottle01Icon,
    etiqueta: CHIP_MEDICACION,
    tono: "info",
    fondo: "bg-info-soft",
    acento: "border-l-info",
  },
  CITA: {
    icono: Stethoscope02Icon,
    etiqueta: CHIP_CITA,
    tono: "acento",
    fondo: "bg-accent-soft",
    acento: "border-l-accent",
  },
} as const

// Borde visible contra el fondo y acento lateral. En alto contraste todo el borde es negro.
const TARJETA =
  "flex w-full items-stretch gap-3 rounded-lg border-2 border-l-8 border-border bg-card p-4 text-left [box-shadow:var(--sombra-sm)] contraste-alto:border-foreground"

type Props = {
  tipo: TipoRecordatorio
  titulo: string
  /** Dato secundario, en gris. */
  secundario?: string
  /** "09:00": solo donde la hora no está ya arriba (la línea de tiempo la pone aparte). */
  hora?: string
  /** Estado como texto con ícono. */
  estado?: ReactNode
  /** Botones. Una tarjeta con `enlace` no lleva botones adentro. */
  acciones?: ReactNode
  /** Si hay enlace, toda la tarjeta es el enlace y lleva una flecha al costado. */
  enlace?: string
  /** Texto visible de la acción del enlace (por ejemplo "Editar"). */
  textoEnlace?: string
}

/**
 * Tarjeta de un recordatorio o de un aviso (Lista, "Para hoy" y detalle del día del calendario):
 * cuadradito con el ícono del tipo sobre fondo suave, acento lateral, etiqueta del tipo, título,
 * dato secundario y estado. Es solo presentación: TarjetaAviso y TarjetaMedicacion le ponen los
 * datos. El que la usa la envuelve en un <li> si va en una lista.
 */
export function TarjetaBase({
  tipo,
  titulo,
  secundario,
  hora,
  estado,
  acciones,
  enlace,
  textoEnlace,
}: Props) {
  const { icono, etiqueta, tono, fondo, acento } = TIPOS[tipo]

  const contenido = (
    <>
      <span
        aria-hidden="true"
        className={cn(
          "flex size-12 shrink-0 items-center justify-center rounded-md text-heading contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card",
          fondo
        )}
      >
        <HugeiconsIcon icon={icono} className="size-6" />
      </span>
      <div className="flex min-w-0 flex-1 flex-col items-start gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Chip tono={tono}>{etiqueta}</Chip>
          {hora ? <span className="font-heading font-medium text-heading">{hora}</span> : null}
        </div>
        <p className="font-bold break-words">{titulo}</p>
        {secundario ? <p className="text-muted-foreground break-words">{secundario}</p> : null}
        {estado}
        {textoEnlace ? <span className="font-semibold text-heading">{textoEnlace}</span> : null}
        {acciones}
      </div>
      {enlace ? (
        <HugeiconsIcon
          icon={ArrowRight01Icon}
          className="size-6 shrink-0 self-center text-heading"
          aria-hidden="true"
        />
      ) : null}
    </>
  )

  if (enlace) {
    return (
      <Link
        to={enlace}
        className={cn(TARJETA, acento, "transition-colors hover:bg-secondary contraste-alto:border-l-foreground")}
      >
        {contenido}
      </Link>
    )
  }
  return <div className={cn(TARJETA, acento, "contraste-alto:border-l-foreground")}>{contenido}</div>
}
