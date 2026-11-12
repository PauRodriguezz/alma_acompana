import { useId } from "react"

import { Button } from "@/components/ui/button"
import { InvitacionHistoria } from "@/features/historia/components/InvitacionHistoria"
import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "@/features/historia/estilos"
import {
  AYUDA_PREGUNTA,
  CONTARTE_ALGO_MAS,
  OTRA_PREGUNTA,
  SALTEAR,
  VOLVER_A_HISTORIA,
} from "@/features/historia/mensajes"
import { cn } from "@/lib/utils"

import { useChat } from "../useChat"

const ESTILO = cn(BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)

/**
 * Lo que se puede tocar debajo del último mensaje de Alma en el modo guion (contrato de historia de vida,
 * 2.2, 2.5 y 11.7): la invitación, los botones de una pregunta, los de una repregunta o los del cierre.
 * Todos son botones grandes con texto. No se muestran mientras Alma está pensando.
 *
 * Foco: estos botones desaparecen al tocarlos. Si se activaron con el teclado (`:focus-visible`), el foco
 * pasa al campo de mensaje para no perder el lugar; con el mouse o el dedo no se toca (en celular abriría el
 * teclado sin que la persona lo pida). "Volver a Historia de vida" no lo necesita: la otra pantalla enfoca su título.
 */
export function AccionesDelGuion() {
  const chat = useChat()
  const idAyuda = useId()
  if (chat.pendiente) return null

  const deshabilitado = chat.ocupadoGuion
  const protegido = (accion: () => void) => (deshabilitado ? undefined : accion)
  const grupo = "flex flex-col gap-3 pl-13 lg:flex-row"

  /** Hace la acción y, si el botón se activó con el teclado, devuelve el foco al campo. */
  const conFoco = (accion: () => void) => () => {
    const activo = document.activeElement
    const conTeclado = activo instanceof HTMLElement && activo.matches(":focus-visible")
    accion()
    if (conTeclado) chat.enfocarCampo()
  }

  if (chat.invitacionPendiente) {
    return (
      <InvitacionHistoria
        onSi={conFoco(chat.aceptarInvitacion)}
        onAhoraNo={conFoco(chat.rechazarInvitacion)}
        deshabilitado={deshabilitado}
      />
    )
  }

  const objetivo = chat.objetivoActivo
  if (objetivo?.tipo === "PREGUNTA") {
    return (
      <div className="flex flex-col gap-3 pl-13">
        <p id={idAyuda} className="text-muted-foreground">
          {AYUDA_PREGUNTA}
        </p>
        <div className="flex flex-col gap-3 lg:flex-row">
          <Button
            variant="secondary"
            aria-disabled={deshabilitado}
            aria-describedby={idAyuda}
            className={ESTILO}
            onClick={protegido(conFoco(chat.otraPregunta))}
          >
            {OTRA_PREGUNTA}
          </Button>
          <Button
            variant="outline"
            aria-disabled={deshabilitado}
            aria-describedby={idAyuda}
            className={ESTILO}
            onClick={protegido(chat.volverAHistoria)}
          >
            {VOLVER_A_HISTORIA}
          </Button>
        </div>
      </div>
    )
  }

  if (objetivo?.tipo === "REPREGUNTA") {
    return (
      <div className={grupo}>
        <Button
          variant="secondary"
          aria-disabled={deshabilitado}
          className={ESTILO}
          onClick={protegido(conFoco(chat.saltearRepregunta))}
        >
          {SALTEAR}
        </Button>
        <Button
          variant="outline"
          aria-disabled={deshabilitado}
          className={ESTILO}
          onClick={protegido(chat.volverAHistoria)}
        >
          {VOLVER_A_HISTORIA}
        </Button>
      </div>
    )
  }

  if (chat.cierrePendiente) {
    return (
      <div className={grupo}>
        <Button
          variant="secondary"
          aria-disabled={deshabilitado}
          className={ESTILO}
          onClick={protegido(conFoco(chat.contarAlgoMas))}
        >
          {CONTARTE_ALGO_MAS}
        </Button>
        <Button
          variant="outline"
          aria-disabled={deshabilitado}
          className={ESTILO}
          onClick={protegido(conFoco(chat.otraPreguntaDespuesDelCierre))}
        >
          {OTRA_PREGUNTA}
        </Button>
      </div>
    )
  }

  return null
}
