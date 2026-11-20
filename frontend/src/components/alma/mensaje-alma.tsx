import * as React from "react"

import { AvatarAlma } from "@/components/alma/avatar-alma"
import { cn } from "@/lib/utils"

/**
 * Mensaje de Alma en el chat (Figma): el avatar de Alma (AvatarAlma, decorativo) y el texto al lado.
 * Lo usan el chat (mensajes, pregunta de ánimo, "Alma está pensando…", guion y "sin pendientes") y el
 * widget de ánimo. Acepta `role="status"` para que el lector de pantalla anuncie el texto.
 */
function MensajeAlma({ className, children, ...props }: React.ComponentProps<"div">) {
  return (
    <div data-slot="mensaje-alma" className={cn("flex items-start gap-3", className)} {...props}>
      <AvatarAlma />
      <div className="min-w-0 flex-1 pt-2">{children}</div>
    </div>
  )
}

export { MensajeAlma }
