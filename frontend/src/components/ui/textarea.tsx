import * as React from "react"
import { cn } from "cn"

// Área de texto con el mismo estilo que el campo (Input): borde de 2 px, foco de 3 px color ring
// y error en destructive. El mensaje de error con ícono va en CampoArea.
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        "min-h-[calc(120rem/17)] w-full min-w-0 resize-y rounded-md border-2 border-input bg-card px-4 py-3 text-body text-foreground outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:shadow-[inset_0_0_0_var(--foco-extra)_var(--ring)] aria-invalid:border-destructive focus-visible:aria-invalid:border-ring disabled:cursor-not-allowed disabled:border-input disabled:bg-disabled disabled:text-disabled-foreground",
        className
      )}
      {...props}
    />
  )
}

export { Textarea }
