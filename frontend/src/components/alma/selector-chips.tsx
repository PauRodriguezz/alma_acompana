import { useId } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Tick02Icon } from "@hugeicons/core-free-icons"

import { MensajeError } from "@/components/alma/mensaje-error"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"

type Opcion<T extends string> = { valor: T; texto: string }

type SelectorChipsProps<T extends string> = {
  /** Texto siempre visible arriba; es el nombre del grupo para el lector de pantalla. */
  etiqueta: string
  opciones: readonly Opcion<T>[]
  valor: T | null | undefined
  alCambiar: (valor: T) => void
  error?: string
  /**
   * eleccion (formularios): la elegida en accent-soft con borde accent.
   * filtro (Figma 51:1539): la elegida rellena de primary; las demás con borde input.
   */
  variante?: "eleccion" | "filtro"
}

// El color de texto de la elegida va con prefijo (data-checked:): sin prefijo, cn descartaría
// text-label por estar en la misma llamada que un color de texto.
const estiloChip = {
  eleccion:
    "border-transparent bg-secondary hover:bg-secondary-hover data-checked:border-accent data-checked:bg-accent-soft contraste-alto:border-foreground contraste-alto:data-checked:shadow-[inset_0_0_0_calc(2rem/17)_var(--foreground)]",
  filtro:
    "border-input bg-card hover:bg-outline-hover data-checked:border-primary data-checked:bg-primary data-checked:text-primary-foreground data-checked:hover:bg-primary-hover",
}

/**
 * Elección de una sola opción con forma de chips (Figma, "Categoría" de 52:1894): píldoras
 * secondary con label semibold; la elegida pasa a accent-soft con borde accent y una tilde
 * (no solo color). Es un grupo de radio de Base UI: Tab entra al grupo, las flechas cambian
 * de opción y Espacio elige. En alto contraste todas llevan borde y la elegida, uno más grueso.
 */
export function SelectorChips<T extends string>({
  etiqueta,
  opciones,
  valor,
  alCambiar,
  error,
  variante = "eleccion",
}: SelectorChipsProps<T>) {
  const id = useId()
  const idEtiqueta = `${id}-etiqueta`
  const idError = `${id}-error`

  return (
    <div className="flex flex-col gap-2">
      <span id={idEtiqueta} className="text-label font-semibold">
        {etiqueta}
      </span>
      <RadioGroup
        aria-labelledby={idEtiqueta}
        aria-describedby={error ? idError : undefined}
        aria-invalid={error ? true : undefined}
        value={valor ?? null}
        onValueChange={(nuevo) => alCambiar(nuevo as T)}
        className="flex flex-wrap gap-2"
      >
        {opciones.map((opcion) => (
          <RadioGroupItem
            key={opcion.valor}
            value={opcion.valor}
            className={`group/chip inline-flex min-h-(--altura-boton) items-center gap-2 rounded-full border-2 px-4 py-2 text-left text-label font-semibold transition-colors ${estiloChip[variante]}`}
          >
            <HugeiconsIcon
              icon={Tick02Icon}
              strokeWidth={2}
              className="hidden size-5 shrink-0 group-data-checked/chip:block"
              aria-hidden="true"
            />
            {opcion.texto}
          </RadioGroupItem>
        ))}
      </RadioGroup>
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}
