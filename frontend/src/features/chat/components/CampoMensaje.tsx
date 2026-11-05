import { useRef, type FormEvent } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { SentIcon } from "@hugeicons/core-free-icons"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

import { ENVIAR, ESCRIBILE_A_ALMA } from "../mensajes"
import { LIMITE_MENSAJE } from "../tipos"
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
  const { borrador, cambiarBorrador, enviar, pendiente } = useChat()
  const campo = useRef<HTMLInputElement>(null)

  function alEnviar(evento: FormEvent) {
    evento.preventDefault()
    if (pendiente) return
    enviar(borrador)
    campo.current?.focus()
  }

  return (
    <form
      onSubmit={alEnviar}
      className="sticky bottom-[calc(var(--nav-inferior)+env(safe-area-inset-bottom))] z-20 mt-auto flex items-start gap-2 bg-background py-2 lg:bottom-0"
    >
      <Input
        ref={campo}
        value={borrador}
        onChange={(evento) => cambiarBorrador(evento.target.value)}
        maxLength={LIMITE_MENSAJE}
        placeholder={ESCRIBILE_A_ALMA}
        aria-label={ESCRIBILE_A_ALMA}
        autoComplete="off"
        enterKeyHint="send"
        className="h-[calc(60rem/17)] flex-1"
      />
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
