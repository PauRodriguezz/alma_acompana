import { useNavigate } from "react-router-dom"

import { MensajeAlma } from "@/components/alma/mensaje-alma"
import { Button } from "@/components/ui/button"
import { BOTON_TEXTO_LARGO } from "@/features/historia/estilos"
import { VER_MI_HISTORIA } from "@/features/historia/mensajes"
import { RUTA_HISTORIA_DE_VIDA } from "@/features/historia/rutas"

/**
 * "Ya respondiste todas las preguntas de tu historia…" con su botón "Ver mi historia de vida" (contrato del
 * mapa de vida, 2.8 b). El botón navega: no deja burbuja (2.9).
 */
export function MensajeSinPendientes({ texto }: { texto: string }) {
  const navegar = useNavigate()
  return (
    <div className="flex flex-col gap-3">
      <MensajeAlma>
        <p className="break-words whitespace-pre-line">{texto}</p>
      </MensajeAlma>
      <div className="pl-13">
        <Button variant="outline" className={BOTON_TEXTO_LARGO} onClick={() => navegar(RUTA_HISTORIA_DE_VIDA)}>
          {VER_MI_HISTORIA}
        </Button>
      </div>
    </div>
  )
}
