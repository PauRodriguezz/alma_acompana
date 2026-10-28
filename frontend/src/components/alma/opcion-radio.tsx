import { type Radio as RadioPrimitive } from "@base-ui/react/radio"
import { HugeiconsIcon } from "@hugeicons/react"
import { Tick02Icon } from "@hugeicons/core-free-icons"
import { cn } from "cn"

import { RadioGroupItem } from "@/components/ui/radio-group"

// Opción de elección del Figma (22:67 y 25:153): tarjeta de un grupo de radios (accesibilidad,
// tipo de recordatorio, frecuencia). Elegida:
// accent-soft + borde accent de 2 px + tilde (no solo color). En alto contraste, borde de
// 4 px en foreground (los 2 px de más se suman con una sombra interior, para que la tarjeta
// no cambie de tamaño).
function OpcionRadio({
  className,
  children,
  ...props
}: RadioPrimitive.Root.Props) {
  return (
    <RadioGroupItem
      className={cn(
        "group/opcion relative flex flex-col rounded-md border-2 border-input bg-card text-left text-foreground transition-colors data-checked:border-accent data-checked:bg-accent-soft contraste-alto:data-checked:border-foreground contraste-alto:data-checked:shadow-[inset_0_0_0_calc(2rem/17)_var(--foreground)]",
        className
      )}
      {...props}
    >
      {children}
    </RadioGroupItem>
  )
}

/** Tilde que solo se ve cuando la opción está elegida. */
function TildeOpcion({ className }: { className?: string }) {
  return (
    <HugeiconsIcon
      icon={Tick02Icon}
      strokeWidth={2}
      className={cn("hidden shrink-0 group-data-checked/opcion:block", className)}
      aria-hidden="true"
    />
  )
}

export { OpcionRadio, TildeOpcion }
