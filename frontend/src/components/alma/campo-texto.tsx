import * as React from "react"
import { cn } from "cn"

import { MensajeError } from "@/components/alma/mensaje-error"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

type CampoTextoProps = Omit<React.ComponentProps<typeof Input>, "id"> & {
  label: string
  /** Mensaje de error en lenguaje simple. Con error el campo queda marcado y se lee con el campo. */
  error?: string
  /** Marca el campo en rojo sin mensaje propio (el motivo se explica en un Aviso aparte). */
  invalido?: boolean
  id?: string
}

// Campo de texto del Figma: etiqueta siempre visible arriba, campo y, si hay error,
// el mensaje con ícono y texto (el color solo nunca alcanza).
function CampoTexto({
  label,
  error,
  invalido,
  id,
  className,
  "aria-describedby": describedByExterno,
  ...props
}: CampoTextoProps) {
  const idGenerado = React.useId()
  const idCampo = id ?? idGenerado
  const idError = `${idCampo}-error`
  // El id del mensaje del campo se suma al que llegue por props (por ejemplo el de un
  // aviso de la pantalla); ninguno pisa al otro.
  const describedBy =
    [error ? idError : undefined, describedByExterno].filter(Boolean).join(" ") ||
    undefined

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={idCampo}>{label}</Label>
      <Input
        id={idCampo}
        aria-invalid={error || invalido ? true : undefined}
        {...props}
        aria-describedby={describedBy}
      />
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}

export { CampoTexto }
