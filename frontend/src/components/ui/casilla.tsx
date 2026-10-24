import * as React from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Tick02Icon } from "@hugeicons/core-free-icons"
import { cn } from "cn"

type CasillaProps = Omit<React.ComponentProps<"input">, "type" | "children"> & {
  /** Texto siempre visible: toda la fila (casilla y texto) marca y desmarca. */
  label: string
}

// Casilla del Figma (Login 112:4484): caja de 28 px, borde de 2 px color foreground, radio 8,
// fondo card; marcada pasa a primary con la tilde en primary-foreground. La fila mide
// al menos 48 px. Es un <input type="checkbox"> nativo, así teclado y lector de pantalla
// funcionan solos (Tab para llegar, Espacio para marcar). Foco: borde de 3 px color ring
// sin separación (4 px negro en alto contraste).
// PROPUESTO (no están en el Figma): foco y deshabilitada, derivados de los del campo de texto
// (deshabilitada: fondo disabled, borde input y texto disabled-foreground).
function Casilla({ label, className, ...props }: CasillaProps) {
  return (
    <label
      data-slot="casilla"
      className={cn(
        "flex min-h-(--altura-boton) cursor-pointer items-center gap-3 text-body text-foreground has-disabled:cursor-not-allowed has-disabled:text-disabled-foreground",
        className
      )}
    >
      <span className="relative inline-flex size-7 shrink-0">
        <input
          type="checkbox"
          className="peer size-7 cursor-[inherit] appearance-none rounded-sm border-2 border-foreground bg-card transition-colors outline-none checked:border-primary checked:bg-primary focus-visible:border-ring focus-visible:shadow-[inset_0_0_0_var(--foco-extra)_var(--ring)] checked:focus-visible:border-ring disabled:border-input disabled:bg-disabled disabled:checked:border-input disabled:checked:bg-disabled"
          {...props}
        />
        <HugeiconsIcon
          icon={Tick02Icon}
          strokeWidth={2}
          className="pointer-events-none absolute inset-0 m-auto size-5 text-primary-foreground opacity-0 peer-checked:opacity-100 peer-disabled:text-disabled-foreground"
          aria-hidden="true"
        />
      </span>
      <span>{label}</span>
    </label>
  )
}

export { Casilla }
