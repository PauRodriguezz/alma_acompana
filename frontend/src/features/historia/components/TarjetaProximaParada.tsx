import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "../estilos"
import { ALMA_TIENE_UNA_PREGUNTA, PROXIMA_PARADA, SEGUIR_ESCRIBIENDO } from "../mensajes"

/**
 * "Próxima parada" (contrato 5.4; formato compacto arriba del mapa, contrato del mapa de vida 2.1): borde punteado,
 * sin el texto de la pregunta. Al tocar "Seguir escribiendo" la página decide qué hacer (abrir el chat con la pregunta).
 */
export function TarjetaProximaParada({
  onSeguir,
  deshabilitado = false,
}: {
  onSeguir: () => void
  deshabilitado?: boolean
}) {
  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-lg border-2 border-dashed border-foreground/60 bg-background/60 p-3 contraste-alto:border-foreground contraste-alto:bg-card">
      <div className="flex min-w-0 flex-1 flex-col basis-40">
        <p className="text-caption text-muted-foreground">{PROXIMA_PARADA}</p>
        <p className="font-semibold text-foreground">{ALMA_TIENE_UNA_PREGUNTA}</p>
      </div>
      <Button
        onClick={deshabilitado ? undefined : onSeguir}
        aria-disabled={deshabilitado}
        className={cn(BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
      >
        {SEGUIR_ESCRIBIENDO}
      </Button>
    </div>
  )
}
