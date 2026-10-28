import * as React from "react"
import { cn } from "cn"

import { MensajeErrorCampo } from "@/components/alma/mensaje-error-campo"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"

type CampoAreaProps = Omit<React.ComponentProps<typeof Textarea>, "id"> & {
  label: string
  /** Mensaje de error en lenguaje simple. Con error el campo queda marcado y se lee con el campo. */
  error?: string
  id?: string
}

// Igual que CampoTexto, para texto largo: etiqueta siempre visible, área y mensaje de error.
function CampoArea({
  label,
  error,
  id,
  className,
  "aria-describedby": describedByExterno,
  ...props
}: CampoAreaProps) {
  const idGenerado = React.useId()
  const idCampo = id ?? idGenerado
  const idError = `${idCampo}-error`
  // Igual que CampoTexto: el id del mensaje se suma al que llegue por props, ninguno pisa al otro.
  const describedBy =
    [error ? idError : undefined, describedByExterno].filter(Boolean).join(" ") ||
    undefined

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={idCampo}>{label}</Label>
      <Textarea
        id={idCampo}
        aria-invalid={error ? true : undefined}
        {...props}
        aria-describedby={describedBy}
      />
      {error ? <MensajeErrorCampo id={idError}>{error}</MensajeErrorCampo> : null}
    </div>
  )
}

export { CampoArea }
