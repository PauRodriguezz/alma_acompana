import { ILUSTRACIONES_ANIMO } from "./ilustraciones"
import { ETIQUETAS_ANIMO } from "./mensajes"
import type { NivelAnimo } from "./tipos"

type Props = {
  /** Nivel elegido: define la carita y la etiqueta. */
  nivelAnimo: NivelAnimo
}

/**
 * Burbuja de la persona con la carita que eligió y su etiqueta debajo (Figma: "Burbuja de chat").
 * No es un botón ni un mensaje que escribió: no se manda al modelo ni se guarda. La ilustración va
 * con alt="" y el texto de la etiqueta hace que se lea como un mensaje más.
 */
export function BurbujaAnimo({ nivelAnimo }: Props) {
  return (
    <div className="flex justify-end">
      <div className="flex flex-col items-center gap-1 rounded-lg bg-accent-soft px-4 py-3 font-semibold text-foreground contraste-alto:border-2 contraste-alto:border-foreground">
        <img src={ILUSTRACIONES_ANIMO[nivelAnimo]} alt="" className="size-10" />
        <span>{ETIQUETAS_ANIMO[nivelAnimo]}</span>
      </div>
    </div>
  )
}
