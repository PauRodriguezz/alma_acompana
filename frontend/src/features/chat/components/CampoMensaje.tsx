import { useState, type FormEvent } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { SentIcon } from "@hugeicons/core-free-icons"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

import { ESCRIBI_TU_RESPUESTA, RESPUESTA_VACIA } from "@/features/historia/mensajes"

import { ENVIAR, ESCRIBILE_A_ALMA } from "../mensajes"
import { LIMITE_MENSAJE, LIMITE_RESPUESTA_GUION } from "../tipos"
import { useChat } from "../useChat"

/**
 * Campo de mensaje y botón de enviar (Figma), fijo al pie del contenido y arriba de la navegación
 * inferior. En celular se separa de la barra de navegación con su alto (`--nav-inferior`); en escritorio
 * no hay barra inferior. Campo y botón miden 60 px (el mínimo del proyecto es 48).
 * - Enter envía. Con el texto vacío (sin contar espacios) no se envía y no se muestra ningún error.
 * - Mientras se espera la respuesta el botón queda con aria-disabled y Enter no envía, así un doble
 *   toque no manda dos veces. El campo sigue habilitado.
 * - Al enviar, el foco se queda en el campo para seguir escribiendo.
 */
export function CampoMensaje() {
  const { borrador, cambiarBorrador, enviar, pendiente, objetivoActivo, campoDeMensaje: campo } = useChat()
  const [avisoVacio, setAvisoVacio] = useState(false)

  // En modo guion el campo admite 1000 caracteres y ayuda a responder (contrato de historia de vida, 5.9).
  const enModoGuion = objetivoActivo !== null
  const nombre = enModoGuion ? ESCRIBI_TU_RESPUESTA : ESCRIBILE_A_ALMA

  function alEnviar(evento: FormEvent) {
    evento.preventDefault()
    if (pendiente) return
    // Fuera del modo guion un envío vacío no hace nada; en modo guion avisa qué hacer (5.8).
    if (enModoGuion && borrador.trim() === "") {
      setAvisoVacio(true)
      campo.current?.focus()
      return
    }
    setAvisoVacio(false)
    enviar(borrador)
    campo.current?.focus()
  }

  return (
    <form
      onSubmit={alEnviar}
      className="sticky bottom-[calc(var(--nav-inferior)+env(safe-area-inset-bottom))] z-20 mt-auto flex items-start gap-2 bg-background py-2 lg:bottom-0"
    >
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <Input
          ref={campo}
          value={borrador}
          onChange={(evento) => {
            cambiarBorrador(evento.target.value)
            setAvisoVacio(false)
          }}
          maxLength={enModoGuion ? LIMITE_RESPUESTA_GUION : LIMITE_MENSAJE}
          placeholder={nombre}
          aria-label={nombre}
          aria-describedby={avisoVacio ? "aviso-respuesta-vacia" : undefined}
          autoComplete="off"
          enterKeyHint="send"
          className="h-[calc(60rem/17)]"
        />
        {avisoVacio ? (
          <p id="aviso-respuesta-vacia" role="alert" className="text-destructive">
            {RESPUESTA_VACIA}
          </p>
        ) : null}
      </div>
      <Button
        type="submit"
        aria-label={ENVIAR}
        aria-disabled={pendiente}
        className="size-[calc(60rem/17)] p-0 aria-disabled:pointer-events-none aria-disabled:translate-y-0 aria-disabled:border-input aria-disabled:bg-disabled aria-disabled:text-disabled-foreground aria-disabled:shadow-none"
      >
        <HugeiconsIcon icon={SentIcon} size={26} aria-hidden="true" />
      </Button>
    </form>
  )
}
