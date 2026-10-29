import * as React from "react"
import { cn } from "cn"

// Área de texto del Figma (Microcápsula nueva, campo "Texto"): mismos bordes, foco y error
// que el campo de texto (input.tsx). Alto mínimo de unas 4 líneas; la persona la puede
// agrandar hacia abajo.
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "flex min-h-[calc(104rem/17)] w-full resize-y rounded-md border-2 border-input bg-card px-4 py-3 text-body outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:shadow-[inset_0_0_0_var(--foco-extra)_var(--ring)] aria-invalid:border-destructive focus-visible:aria-invalid:border-ring disabled:cursor-not-allowed disabled:border-input disabled:bg-disabled disabled:text-disabled-foreground disabled:placeholder:text-disabled-foreground",
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
