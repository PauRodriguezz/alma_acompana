import { useRef, useState } from "react"

import { Button } from "@/components/ui/button"

import {
  BOTON_EMPEZAR_DE_NUEVO,
  BOTON_LISTO,
  ORDENAR_INDICACION,
  textoPasoNumero,
} from "../mensajes"
import type { OpcionJuego } from "../tipos"

/**
 * ORDENAR (contrato, 10.2; el Figma no lo dibuja): se toca cada paso en el orden que se quiere y
 * muestra su número; tocarlo de nuevo lo quita y los siguientes se corren. Sin arrastrar
 * (accesibilidad). "Listo" se habilita con todos ordenados; "Empezar de nuevo" borra el orden.
 * Los pasos quedan siempre en el lugar en que llegaron: lo que cambia es el número.
 */
export function PasosOrdenar({
  pasos,
  bloqueados,
  respondido,
  onListo,
}: {
  pasos: OpcionJuego[]
  /** Mientras viaja la respuesta y después del resultado. */
  bloqueados: boolean
  /** Con el resultado ya no se muestran los botones (el panel trae "Continuar"). */
  respondido: boolean
  onListo: (ids: number[]) => void
}) {
  const [orden, setOrden] = useState<number[]>([])
  const primerPasoRef = useRef<HTMLButtonElement>(null)
  const completo = orden.length === pasos.length

  function tocar(id: number) {
    if (bloqueados) return
    setOrden((actual) => (actual.includes(id) ? actual.filter((x) => x !== id) : [...actual, id]))
  }

  function empezarDeNuevo() {
    setOrden([])
    primerPasoRef.current?.focus()
  }

  return (
    <>
      {respondido ? null : <p className="text-muted-foreground">{ORDENAR_INDICACION}</p>}

      <ul className="flex flex-col gap-4">
        {pasos.map((paso, i) => {
          const posicion = orden.indexOf(paso.id)
          const numero = posicion === -1 ? null : posicion + 1
          return (
            <li key={paso.id}>
              <button
                ref={i === 0 ? primerPasoRef : undefined}
                type="button"
                aria-pressed={numero !== null}
                aria-disabled={bloqueados}
                onClick={() => tocar(paso.id)}
                className={`flex min-h-(--altura-campo) w-full items-center gap-3 rounded-md border-2 px-4 py-3 text-left text-foreground transition-colors aria-disabled:cursor-default contraste-alto:border-foreground ${
                  bloqueados ? "" : "cursor-pointer hover:bg-outline-hover"
                } ${numero !== null ? "border-heading bg-secondary contraste-alto:bg-card" : "border-input bg-card"}`}
              >
                <span
                  aria-hidden="true"
                  className={`inline-flex size-[calc(36rem/17)] shrink-0 items-center justify-center rounded-full border-2 font-heading text-button font-medium ${
                    numero !== null
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-input bg-card"
                  }`}
                >
                  {numero}
                </span>
                {paso.imagen ? (
                  <img
                    src={paso.imagen}
                    alt=""
                    className="size-[calc(64rem/17)] shrink-0 rounded-sm object-cover"
                  />
                ) : null}
                <span className="min-w-0 flex-1 wrap-break-word">
                  {paso.texto}
                  {numero !== null ? <span className="sr-only">{textoPasoNumero(numero)}</span> : null}
                </span>
              </button>
            </li>
          )
        })}
      </ul>

      {respondido ? null : (
        <div className="grid gap-3 lg:grid-cols-2">
          <Button variant="outline" className="w-full" onClick={empezarDeNuevo} disabled={bloqueados}>
            {BOTON_EMPEZAR_DE_NUEVO}
          </Button>
          <Button
            className="w-full"
            onClick={() => onListo(orden)}
            disabled={!completo || bloqueados}
          >
            {BOTON_LISTO}
          </Button>
        </div>
      )}
    </>
  )
}
