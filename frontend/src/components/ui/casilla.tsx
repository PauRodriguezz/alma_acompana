import { type Checkbox as CheckboxPrimitive } from "@base-ui/react/checkbox"
import { cn } from "@/lib/utils"

import { Checkbox } from "./checkbox"

type CasillaProps = Omit<CheckboxPrimitive.Root.Props, "children"> & {
  /** Texto siempre visible: toda la fila (cajita y texto) marca y desmarca. */
  label: string
}

// Casilla del Figma: agrupa lo que la cajita (Checkbox) no resuelve sola, igual que
// CampoTexto agrupa Input y Label. La fila entera es un <label> de al menos 48 px, así un
// clic en el texto marca y desmarca, y el texto queda asociado a la casilla para el lector
// de pantalla. Teclado: Tab llega a la cajita y Espacio la marca.
function Casilla({ label, className, disabled, ...props }: CasillaProps) {
  return (
    <label
      data-slot="casilla"
      className={cn(
        "flex min-h-(--altura-boton) cursor-pointer items-center gap-3 text-body text-foreground",
        disabled && "cursor-not-allowed text-disabled-foreground",
        className
      )}
    >
      <Checkbox disabled={disabled} {...props} />
      <span>{label}</span>
    </label>
  )
}

export { Casilla }
