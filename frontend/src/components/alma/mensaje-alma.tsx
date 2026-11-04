import * as React from "react"

import { cn } from "@/lib/utils"

/**
 * Mensaje de Alma en el chat (Figma): un círculo rosa de avatar y el texto al lado. Lo usan el
 * widget de ánimo, el Inicio provisorio y, después, el Chat. El avatar es el círculo liso del Figma
 * (todavía sin ilustración). Acepta `role="status"` para que el lector de pantalla anuncie el texto.
 */
function MensajeAlma({ className, children, ...props }: React.ComponentProps<"div">) {
  return (
    <div data-slot="mensaje-alma" className={cn("flex items-start gap-3", className)} {...props}>
      <span
        aria-hidden="true"
        className="size-10 shrink-0 rounded-full bg-accent-soft contraste-alto:border-2 contraste-alto:border-foreground"
      />
      <div className="min-w-0 flex-1 pt-2">{children}</div>
    </div>
  )
}

export { MensajeAlma }
