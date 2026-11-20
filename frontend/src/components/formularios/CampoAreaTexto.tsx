import * as React from "react"
import { cn } from "@/lib/utils"

import { MensajeError } from "@/components/formularios/MensajeError"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { CAMPO_OPCIONAL } from "@/lib/mensajes"

type CampoAreaTextoProps = Omit<React.ComponentProps<typeof Textarea>, "id"> & {
  label: string
  /** Agrega "(opcional)" a la etiqueta. */
  opcional?: boolean
  /** Texto de ayuda debajo de la etiqueta (se lee con el campo). */
  ayuda?: string
  error?: string
  id?: string
  /** Barra de herramientas pegada arriba del campo, debajo de la etiqueta y la ayuda. */
  barra?: React.ReactNode
}

// Igual que CampoTexto pero con un área de texto: etiqueta siempre visible arriba, ayuda
// opcional y, si hay error, el mensaje con ícono y texto.
function CampoAreaTexto({
  label,
  opcional,
  ayuda,
  error,
  id,
  barra,
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
        {opcional ? <span className="font-normal text-muted-foreground">{CAMPO_OPCIONAL}</span> : null}
      </Label>
      {ayuda ? (
        <p id={idAyuda} className="text-muted-foreground">
          {ayuda}
        </p>
      ) : null}
      {/* Con barra (editor de texto), barra y campo forman un solo bloque: sin espacio y sin borde redondeado arriba del campo */}
      <div className="flex flex-col">
        {barra}
        <Textarea
          id={idCampo}
          aria-invalid={error ? true : undefined}
          {...props}
          className={barra ? "rounded-t-none" : undefined}
          aria-describedby={describedBy}
        />
      </div>
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}

export { CampoAreaTexto }
