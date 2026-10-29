import type { ReactNode } from "react"
import { cn } from "@/lib/utils"

/**
 * Aspecto de un día de calendario (lo comparten la grilla del mes y la tira de la semana): el
 * número en un círculo y, si el día tiene una cita, un punto abajo. El día elegido lleva círculo
 * lleno y negrita; "hoy", un anillo. En alto contraste, negro. El punto es decorativo: quien usa
 * el día agrega a su nombre accesible el texto que lo explica (el color solo no alcanza).
 * Tiene que ir dentro de un contenedor `relative`.
 */
export function AspectoDia({
  esHoy,
  seleccionado,
  marcado,
  children,
}: {
  esHoy: boolean
  seleccionado: boolean
  marcado: boolean
  children: ReactNode
}) {
  return (
    <>
      <span
        className={cn(
          "flex size-[calc(40rem/17)] items-center justify-center rounded-full text-foreground",
          esHoy && !seleccionado && "font-bold ring-2 ring-heading contraste-alto:ring-foreground",
          seleccionado &&
            "bg-accent font-bold text-accent-foreground contraste-alto:bg-foreground contraste-alto:text-background"
        )}
      >
        {children}
      </span>
      {marcado ? (
        <span
          aria-hidden="true"
          className="absolute bottom-0 left-1/2 size-1.5 -translate-x-1/2 rounded-full bg-accent contraste-alto:bg-foreground"
        />
      ) : null}
    </>
  )
}
