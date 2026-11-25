import * as React from "react"

import { CampoFormulario } from "@/components/formularios/CampoTexto"
import { Textarea } from "@/components/ui/textarea"

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
  return (
    <CampoFormulario
      label={label}
      opcional={opcional}
      ayuda={ayuda}
      error={error}
      id={id}
      describedBy={describedByExterno}
      className={className}
    >
      {(campo) => (
        // Con barra (editor de texto), barra y campo forman un solo bloque: sin espacio y sin borde redondeado arriba del campo
        <div className="flex flex-col">
          {barra}
          <Textarea
            id={campo.id}
            aria-invalid={error ? true : undefined}
            {...props}
            className={barra ? "rounded-t-none" : undefined}
            aria-describedby={campo.describedBy}
          />
        </div>
      )}
    </CampoFormulario>
  )
}

export { CampoAreaTexto }
