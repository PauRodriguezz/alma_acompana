import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "../estilos"
import { ALMA_TIENE_UNA_PREGUNTA, PROXIMA_PARADA, SEGUIR_ESCRIBIENDO } from "../mensajes"

/**
 * "Próxima parada" (Figma, contrato 5.4): borde punteado, sin el texto de la pregunta. Al tocar
 * "Seguir escribiendo" la página decide qué hacer (abrir el chat con la pregunta).
 */
export function TarjetaProximaParada({
  onSeguir,
  deshabilitado = false,
}: {
  onSeguir: () => void
  deshabilitado?: boolean
}) {
  return (
    <div className="flex flex-col gap-2 rounded-lg border-2 border-dashed border-foreground/60 bg-background/60 p-4 contraste-alto:border-foreground contraste-alto:bg-card">
      <p className="text-caption text-muted-foreground">{PROXIMA_PARADA}</p>
      <p className="font-semibold text-foreground">{ALMA_TIENE_UNA_PREGUNTA}</p>
      <Button
        onClick={deshabilitado ? undefined : onSeguir}
        aria-disabled={deshabilitado}
        className={cn("w-full", BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
      >
        {SEGUIR_ESCRIBIENDO}
      </Button>
    </div>
  )
}
