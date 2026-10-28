import * as React from "react"
import { Input as InputPrimitive } from "@base-ui/react/input"
import { cn } from "cn"

// Campo del Figma: 55,2 px de alto, borde de 2 px (input, 3,6:1); con foco el borde pasa
// a 3 px de color ring; con error, borde destructive (el mensaje con ícono va en CampoTexto).
function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <InputPrimitive
      type={type}
      data-slot="input"
      className={cn(
        "h-(--altura-campo) w-full min-w-0 rounded-md border-2 border-input bg-card px-4 text-body text-foreground outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:shadow-[inset_0_0_0_var(--foco-extra)_var(--ring)] aria-invalid:border-destructive focus-visible:aria-invalid:border-ring disabled:cursor-not-allowed disabled:border-input disabled:bg-disabled disabled:text-disabled-foreground disabled:placeholder:text-disabled-foreground",
        className
      )}
      {...props}
    />
  )
}

export { Input }
