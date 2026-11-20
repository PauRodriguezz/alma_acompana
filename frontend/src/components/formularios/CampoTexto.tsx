import * as React from "react"
import { cn } from "@/lib/utils"

import { MensajeError } from "@/components/formularios/MensajeError"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { CAMPO_OPCIONAL } from "@/lib/mensajes"

type CampoFormularioProps = {
  label: string
  /** Agrega "(opcional)" a la etiqueta. */
  opcional?: boolean
  /** Texto de ayuda debajo de la etiqueta (se lee con el campo). */
  ayuda?: string
  /** Mensaje de error en lenguaje simple. Con error el campo queda marcado y se lee con el campo. */
  error?: string
  id?: string
  /** Ids que llegan por props (por ejemplo el de un aviso de la pantalla): se suman a los de la ayuda y el error. */
  describedBy?: string
  className?: string
  /** Dibuja el control (input, área de texto) con el `id` y el `aria-describedby` que le corresponden. */
  children: (campo: { id: string; describedBy: string | undefined }) => React.ReactNode
}

/**
 * Lo que comparten todos los campos de texto: etiqueta siempre visible arriba, ayuda opcional, el control y, si
 * hay error, el mensaje con ícono y texto (el color solo nunca alcanza). Calcula el id del control y el
 * `aria-describedby` (ayuda, error y el que llegue de afuera; ninguno pisa al otro).
 */
function CampoFormulario({
  label,
  opcional,
  ayuda,
  error,
  id,
  describedBy: describedByExterno,
  className,
  children,
}: CampoFormularioProps) {
  const idGenerado = React.useId()
  const idCampo = id ?? idGenerado
  const idAyuda = `${idCampo}-ayuda`
  const idError = `${idCampo}-error`
  const describedBy =
    [ayuda ? idAyuda : undefined, error ? idError : undefined, describedByExterno].filter(Boolean).join(" ") ||
    undefined

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={idCampo}>
        {label}
        {opcional ? <span className="font-normal text-muted-foreground">{CAMPO_OPCIONAL}</span> : null}
      </Label>
      {ayuda ? (
        <p id={idAyuda} className="text-muted-foreground">
          {ayuda}
        </p>
      ) : null}
      {children({ id: idCampo, describedBy })}
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}

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
  return (
    <CampoFormulario label={label} error={error} id={id} describedBy={describedByExterno} className={className}>
      {(campo) => (
        <Input
          id={campo.id}
          aria-invalid={error || invalido ? true : undefined}
          {...props}
          aria-describedby={campo.describedBy}
        />
      )}
    </CampoFormulario>
  )
}

export { CampoFormulario, CampoTexto }
