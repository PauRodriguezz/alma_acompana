import { useEffect, useRef, useState, type RefObject } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { BookOpen01Icon } from "@hugeicons/core-free-icons"

import { Button } from "@/components/ui/button"
import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "@/features/historia/estilos"
import { SEGUIR_MI_HISTORIA } from "@/features/historia/mensajes"
import { useEsEscritorio } from "@/lib/useEsEscritorio"
import { cn } from "@/lib/utils"

import { useChat } from "../useChat"

/** El campo de mensaje tiene el foco (en un celular, el teclado está abierto). */
function useCampoConFoco(campo: RefObject<HTMLInputElement | null>): boolean {
  const [conFoco, setConFoco] = useState(() => document.activeElement === campo.current && campo.current !== null)
  useEffect(() => {
    const alEntrar = (evento: FocusEvent) => {
      if (evento.target === campo.current) setConFoco(true)
    }
    const alSalir = (evento: FocusEvent) => {
      if (evento.target === campo.current) setConFoco(false)
    }
    document.addEventListener("focusin", alEntrar)
    document.addEventListener("focusout", alSalir)
    return () => {
      document.removeEventListener("focusin", alEntrar)
      document.removeEventListener("focusout", alSalir)
    }
  }, [campo])
  return conFoco
}

/**
 * Botón fijo "Seguir mi historia de vida" (contrato del mapa de vida, 2.8 a). Se ve sin objetivo activo, sin alarma
 * en la visita y con preguntas pendientes. Pide la próxima pregunta y deja una burbuja de la persona con su texto.
 *
 * - Desde `lg` (escritorio): botón de texto, en el flujo, justo encima del campo de mensaje.
 * - Por debajo de `lg`: círculo de 56 px con el ícono de Historia de vida, flotando arriba del campo (nunca encima),
 *   a la derecha. Al tocarlo se despliega a su izquierda el botón completo. Se oculta mientras el campo tiene el foco,
 *   para no tapar el teclado.
 *
 * Foco: al activarlo con el teclado, el foco pasa al campo de mensaje, porque el botón desaparece cuando llega la
 * pregunta (con el dedo o el mouse no se toca, para no abrir el teclado sin que la persona lo pida).
 */
export function BotonSeguirHistoria() {
  const chat = useChat()
  const esEscritorio = useEsEscritorio()
  const escribiendo = useCampoConFoco(chat.campoDeMensaje)
  if (!chat.mostrarBotonSeguirHistoria) return null

  const deshabilitado = chat.pendiente || chat.ocupadoGuion

  function seguir() {
    if (deshabilitado) return
    const activo = document.activeElement
    const conTeclado = activo instanceof HTMLElement && activo.matches(":focus-visible")
    chat.seguirMiHistoria()
    if (conTeclado) chat.enfocarCampo()
  }

  if (esEscritorio) {
    return (
      <Button
        type="button"
        variant="secondary"
        aria-disabled={deshabilitado}
        onClick={seguir}
        className={cn("self-start", BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
      >
        {SEGUIR_MI_HISTORIA}
      </Button>
    )
  }
  // Con el teclado abierto no se muestra. (Al ocultarse se desmonta: al volver arranca plegado.)
  if (escribiendo) return null
  return <BotonFlotante deshabilitado={deshabilitado} onSeguir={seguir} />
}

/** El círculo y, desplegado, el botón completo a su izquierda. */
function BotonFlotante({ deshabilitado, onSeguir }: { deshabilitado: boolean; onSeguir: () => void }) {
  const [desplegado, setDesplegado] = useState(false)
  const botonCompleto = useRef<HTMLButtonElement>(null)
  const circulo = useRef<HTMLButtonElement>(null)

  // Al desplegarlo, el foco pasa al botón completo para poder activarlo enseguida con el teclado.
  useEffect(() => {
    if (desplegado) botonCompleto.current?.focus()
  }, [desplegado])

  return (
    <div
      className="absolute right-0 bottom-full mb-2 flex max-w-full items-end justify-end gap-2"
      onKeyDown={(evento) => {
        if (evento.key === "Escape" && desplegado) {
          setDesplegado(false)
          circulo.current?.focus()
        }
      }}
    >
      {desplegado ? (
        <Button
          ref={botonCompleto}
          type="button"
          variant="secondary"
          id="boton-seguir-historia"
          aria-disabled={deshabilitado}
          onClick={() => {
            onSeguir()
            setDesplegado(false)
          }}
          className={cn("min-w-0 shrink [box-shadow:var(--sombra-tarjeta)]", BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
        >
          {SEGUIR_MI_HISTORIA}
        </Button>
      ) : null}
      <button
        ref={circulo}
        type="button"
        aria-label={SEGUIR_MI_HISTORIA}
        aria-expanded={desplegado}
        aria-controls="boton-seguir-historia"
        onClick={() => setDesplegado((actual) => !actual)}
        className={cn(
          "flex size-[calc(56rem/17)] shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground [box-shadow:var(--sombra-tarjeta)]",
          "hover:bg-primary-hover contraste-alto:border-2 contraste-alto:border-foreground"
        )}
      >
        <HugeiconsIcon icon={BookOpen01Icon} className="size-7" aria-hidden="true" />
      </button>
    </div>
  )
}
