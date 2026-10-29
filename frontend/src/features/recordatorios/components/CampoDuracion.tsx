import { useId } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { MinusSignIcon, PlusSignIcon } from "@hugeicons/core-free-icons"
import { cn } from "@/lib/utils"

import { MensajeError } from "@/components/alma/mensaje-error"
import { buttonVariants } from "@/components/ui/button-variants"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

import { ETIQUETA_DURACION, MAS_DIAS, MENOS_DIAS, UNIDAD_DIAS } from "../mensajes"

const MINIMO = 1
const MAXIMO = 365

const BOTON = cn(
  buttonVariants({ variant: "outline" }),
  "size-(--altura-boton) px-0 disabled:opacity-100"
)

type Props = {
  /** NaN o vacío mientras la persona escribe o borra el número. */
  valor: number
  onChange: (valor: number) => void
  onBlur?: () => void
  error?: string
}

/**
 * Duración en días (1 a 365): número editable entre los botones "Menos días" y "Más días", para
 * poder escribir 90 sin tocar 59 veces. Etiqueta siempre visible; el error va con ícono y texto.
 */
export function CampoDuracion({ valor, onChange, onBlur, error }: Props) {
  const idEtiqueta = useId()
  const idCampo = useId()
  const idError = `${idCampo}-error`
  const hayNumero = Number.isFinite(valor)

  function mover(paso: number) {
    // Sin número (campo vacío), cualquiera de los dos botones arranca en 1.
    const base = hayNumero ? valor : 0
    onChange(Math.min(MAXIMO, Math.max(MINIMO, base + paso)))
  }

  return (
    <div className="flex flex-col gap-2">
      <Label id={idEtiqueta} htmlFor={idCampo}>
        {ETIQUETA_DURACION}
      </Label>
      <div className="flex items-center gap-3">
        <button
          type="button"
          className={BOTON}
          aria-label={MENOS_DIAS}
          disabled={hayNumero && valor <= MINIMO}
          onClick={() => mover(-1)}
        >
          <HugeiconsIcon icon={MinusSignIcon} className="size-6" aria-hidden="true" />
        </button>
        <Input
          id={idCampo}
          type="number"
          inputMode="numeric"
          min={MINIMO}
          max={MAXIMO}
          value={hayNumero ? valor : ""}
          onChange={(evento) =>
            onChange(evento.target.value === "" ? Number.NaN : Number(evento.target.value))
          }
          onBlur={onBlur}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? idError : undefined}
          className="w-24 text-center font-bold"
        />
        <span aria-hidden="true" className="font-semibold">
          {UNIDAD_DIAS}
        </span>
        <button
          type="button"
          className={BOTON}
          aria-label={MAS_DIAS}
          disabled={hayNumero && valor >= MAXIMO}
          onClick={() => mover(1)}
        >
          <HugeiconsIcon icon={PlusSignIcon} className="size-6" aria-hidden="true" />
        </button>
      </div>
      {error ? <MensajeError id={idError}>{error}</MensajeError> : null}
    </div>
  )
}
