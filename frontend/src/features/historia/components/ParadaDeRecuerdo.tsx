import { Link } from "react-router-dom"

import { formatearAnio } from "../anio"
import { rutaDeRecuerdo } from "../rutas"
import type { RecuerdoResumen } from "../tipos"

/**
 * El recuerdo de una parada (Figma): año arriba y título abajo, en una tarjeta que es un enlace al
 * detalle. El nombre accesible es "título, año" (11.7); el número de la parada es decorativo y lo
 * dibuja `CaminoDeRecuerdos`.
 */
export function ParadaDeRecuerdo({ recuerdo }: { recuerdo: RecuerdoResumen }) {
  const anio = formatearAnio(recuerdo.precision, recuerdo.anio)
  return (
    <Link
      to={rutaDeRecuerdo(recuerdo.id)}
      aria-label={anio ? `${recuerdo.titulo}, ${anio}` : recuerdo.titulo}
      className="flex min-h-[calc(60rem/17)] flex-col justify-center rounded-md bg-card px-3 py-2 shadow-sm contraste-alto:border-2 contraste-alto:border-foreground"
    >
      {anio ? <span className="text-caption text-muted-foreground">{anio}</span> : null}
      <span className="font-semibold wrap-break-word text-foreground">{recuerdo.titulo}</span>
    </Link>
  )
}
