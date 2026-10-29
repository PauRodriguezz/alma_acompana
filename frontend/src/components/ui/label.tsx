import * as React from "react"
import { cn } from "@/lib/utils"

// Etiqueta del Figma (alma/label): siempre visible, nunca solo un placeholder.
function Label({ className, ...props }: React.ComponentProps<"label">) {
  return (
    <label
      data-slot="label"
      className={cn(
        "block text-label font-semibold text-foreground select-none peer-disabled:cursor-not-allowed",
        className
      )}
      {...props}
    />
  )
}

export { Label }
