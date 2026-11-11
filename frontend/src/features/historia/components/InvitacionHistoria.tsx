import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "../estilos"
import { INVITACION_AHORA_NO, INVITACION_SI } from "../mensajes"

/**
 * Los botones de la invitación del chat (contrato, 2.2 y 11.7). El texto "¿Seguimos completando tu
 * historia de vida?" es un mensaje de Alma más de la conversación; acá solo van las dos respuestas.
 */
export function InvitacionHistoria({
  onSi,
  onAhoraNo,
  deshabilitado,
}: {
  onSi: () => void
  onAhoraNo: () => void
  deshabilitado: boolean
}) {
  return (
    <div className="flex flex-col gap-3 pl-13 lg:flex-row">
      <Button
        onClick={deshabilitado ? undefined : onSi}
        aria-disabled={deshabilitado}
        className={cn(BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
      >
        {INVITACION_SI}
      </Button>
      <Button
        variant="outline"
        onClick={deshabilitado ? undefined : onAhoraNo}
        aria-disabled={deshabilitado}
        className={cn(BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
      >
        {INVITACION_AHORA_NO}
      </Button>
    </div>
  )
}
