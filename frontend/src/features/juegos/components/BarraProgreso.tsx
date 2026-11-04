import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { RUTA_JUEGOS } from "@/features/entretenimiento/rutas"

import { PROGRESO_ETIQUETA, textoProgreso, VOLVER } from "../mensajes"

/**
 * Barra superior del ejercicio (Figma 134:5168): "‹ Volver", la barra de avance y "N de 10".
 * Salir no llama a nada ni pide confirmación (contrato, sección 2). Con letra grande, la barra
 * baja a una segunda línea en vez de achicarse.
 */
export function BarraProgreso({ posicion, total }: { posicion: number; total: number }) {
  const texto = textoProgreso(posicion, total)
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
      <EnlaceVolver to={RUTA_JUEGOS} texto={VOLVER} />
      <div className="flex min-w-[calc(160rem/17)] flex-1 items-center gap-3">
        <div
          role="progressbar"
          aria-label={PROGRESO_ETIQUETA}
          aria-valuemin={0}
          aria-valuemax={total}
          aria-valuenow={posicion}
          aria-valuetext={texto}
          className="h-[calc(16rem/17)] flex-1 overflow-hidden rounded-full bg-border contraste-alto:border-2 contraste-alto:border-foreground"
        >
          <div
            className="h-full rounded-full bg-success"
            style={{ width: `${Math.min(100, (posicion / total) * 100)}%` }}
          />
        </div>
        {/* El lector de pantalla ya lo lee en la barra (aria-valuetext) */}
        <span aria-hidden="true" className="font-bold whitespace-nowrap text-heading">
          {texto}
        </span>
      </div>
    </div>
  )
}
