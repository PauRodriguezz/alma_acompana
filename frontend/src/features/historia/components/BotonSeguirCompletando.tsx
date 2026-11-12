import { ALMA_PENSANDO } from "@/features/chat/mensajes"
import { cn } from "@/lib/utils"

import { SEGUIR_COMPLETANDO } from "../mensajes"

/**
 * Botón de texto "Seguir completando este recuerdo" (contrato del mapa de vida, 2.3 y 2.6): al menos 44 px de alto.
 * Mientras Alma busca la repregunta queda con `aria-disabled` y dice "Alma está pensando…".
 */
export function BotonSeguirCompletando({
  onClick,
  esperando,
  deshabilitado,
  className,
}: {
  onClick: () => void
  esperando: boolean
  deshabilitado: boolean
  className?: string
}) {
  return (
    <button
      type="button"
      aria-disabled={deshabilitado}
      onClick={deshabilitado ? undefined : onClick}
      className={cn(
        "inline-flex min-h-[2.75rem] items-center self-start rounded-md font-semibold text-primary underline-offset-4 hover:underline contraste-alto:text-foreground contraste-alto:underline",
        "aria-disabled:cursor-default aria-disabled:text-muted-foreground aria-disabled:no-underline",
        className
      )}
    >
      {esperando ? ALMA_PENSANDO : SEGUIR_COMPLETANDO}
    </button>
  )
}
