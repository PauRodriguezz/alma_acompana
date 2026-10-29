import * as React from "react"
import { cn } from "cn"

import { MensajeError } from "@/components/alma/mensaje-error"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"

type CampoAreaTextoProps = Omit<React.ComponentProps<typeof Textarea>, "id"> & {
  label: string
  /** Agrega "(opcional)" a la etiqueta. */
  opcional?: boolean
  /** Texto de ayuda debajo de la etiqueta (se lee con el campo). */
  ayuda?: string
  error?: string
  id?: string
}

// Igual que CampoTexto pero con un área de texto: etiqueta siempre visible arriba, ayuda
// opcional y, si hay error, el mensaje con ícono y texto.
function CampoAreaTexto({
  label,
  opcional,
  ayuda,
  error,
  id,
  className,
  "aria-describedby": describedByExterno,
  ...props
}: CampoAreaTextoProps) {
  const idGenerado = React.useId()
  const idCampo = id ?? idGenerado
  const idAyuda = `${idCampo}-ayuda`
  const idError = `${idCampo}-error`
  const describedBy =
    [ayuda ? idAyuda : undefined, error ? idError : undefined, describedByExterno]
      .filter(Boolean)
      .join(" ") || undefined

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={idCampo}>
        {label}
        {opcional ? <span className="font-normal text-muted-foreground"> (opcional)</span> : null}
      </Label>
      {ayuda ? (
        <p id={idAyuda} className="text-muted-foreground">
          {ayuda}
        </p>
      ) : null}
      <Textarea
        id={idCampo}
        aria-invalid={error ? true : undefined}
        {...props}
        aria-describedby={describedBy}
      />
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}

export { CampoAreaTexto }
