import { ILUSTRACIONES_ANIMO } from "./ilustraciones"
import { ETIQUETAS_ANIMO } from "./mensajes"
import type { NivelAnimo } from "./tipos"

type Props = {
  nivel: NivelAnimo
  /** Mientras se guarda: no reacciona a toques (aria-disabled, sin quitarle el foco). */
  deshabilitada: boolean
  onElegir: (nivel: NivelAnimo) => void
}

/**
 * Una carita del widget (Figma: "Carita de ánimo"): botón con la ilustración y su etiqueta siempre
 * visible. Su nombre accesible es la etiqueta (la imagen va con alt=""). La ilustración mide entre
 * 48 px (mínimo fijo) y 54 px (Figma) y se achica para entrar en su columna con letra "Muy grande".
 * Con alto contraste lleva borde.
 */
export function CaritaAnimo({ nivel, deshabilitada, onElegir }: Props) {
  return (
    <button
      type="button"
      aria-disabled={deshabilitada || undefined}
      onClick={() => {
        if (!deshabilitada) onElegir(nivel)
      }}
      className="flex min-w-0 flex-col items-center gap-2 rounded-md border-2 border-transparent px-0.5 py-1 text-center font-semibold text-foreground hover:bg-secondary contraste-alto:border-foreground aria-disabled:cursor-wait aria-disabled:opacity-60 aria-disabled:hover:bg-transparent"
    >
      <img
        src={ILUSTRACIONES_ANIMO[nivel]}
        alt=""
        className="aspect-square w-[min(calc(54rem/17),100%)] min-w-[48px]"
      />
      <span className="max-w-full break-words">{ETIQUETAS_ANIMO[nivel]}</span>
    </button>
  )
}
