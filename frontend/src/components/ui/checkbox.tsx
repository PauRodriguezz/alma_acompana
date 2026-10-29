import { Checkbox as CheckboxPrimitive } from "@base-ui/react/checkbox"
import { cn } from "@/lib/utils"
import { HugeiconsIcon } from "@hugeicons/react"
import { Tick02Icon } from "@hugeicons/core-free-icons"

// Cajita de la casilla del Figma (Login 112:4484): 28 px, borde de 2 px color foreground,
// radio 8, fondo card; marcada pasa a primary con la tilde en primary-foreground. Foco: borde
// de 3 px color ring sin separación (4 px negro en alto contraste).
// PROPUESTO (no están en el Figma): foco y deshabilitada, derivados de los del campo de texto
// (deshabilitada: fondo disabled, borde input y tilde disabled-foreground).
// Para usarla con etiqueta y fila clickeable, ver components/ui/casilla.tsx.
function Checkbox({ className, ...props }: CheckboxPrimitive.Root.Props) {
  return (
    <CheckboxPrimitive.Root
      data-slot="checkbox"
      className={cn(
        "peer relative flex size-7 shrink-0 items-center justify-center rounded-sm border-2 border-foreground bg-card text-primary-foreground transition-colors outline-none focus-visible:border-ring focus-visible:shadow-[inset_0_0_0_var(--foco-extra)_var(--ring)] data-checked:border-primary data-checked:bg-primary data-checked:focus-visible:border-ring data-disabled:cursor-not-allowed data-disabled:border-input data-disabled:bg-disabled data-disabled:text-disabled-foreground",
        className
      )}
      {...props}
    >
      <CheckboxPrimitive.Indicator
        data-slot="checkbox-indicator"
        className="grid place-content-center text-current"
      >
        <HugeiconsIcon
          icon={Tick02Icon}
          strokeWidth={2}
          className="size-5"
          aria-hidden="true"
        />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  )
}

export { Checkbox }
