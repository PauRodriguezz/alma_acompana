import { CAMBIAR_ANIMO } from "./mensajes"

type Props = {
  /** El padre vuelve a mostrar el widget con el registroId (5.6). */
  onClick: () => void
}

/**
 * "Cambiar cómo me siento" (Figma), debajo de la pregunta de Alma. Zona táctil de al menos 44 px.
 * Lo muestra el padre y lo quita cuando la persona manda su primer mensaje.
 */
export function EnlaceCambiarAnimo({ onClick }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex min-h-[calc(44rem/17)] items-center self-start font-semibold text-heading underline underline-offset-4"
    >
      {CAMBIAR_ANIMO}
    </button>
  )
}
