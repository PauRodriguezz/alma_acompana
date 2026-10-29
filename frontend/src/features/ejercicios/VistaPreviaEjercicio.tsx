import { useId } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Tick02Icon } from "@hugeicons/core-free-icons"

import { Card } from "@/components/ui/card"
import type { CategoriaEjercicio, FormatoEjercicio } from "@/features/admin/etiquetas"

import { EtiquetaCategoriaEjercicio } from "./EtiquetaCategoriaEjercicio"
import type { OpcionFormulario } from "./schemas"
import { ADMIN_VISTA_PREVIA } from "@/features/admin/mensajes"
import {
  PREVIA_CONSIGNA,
  PREVIA_CORRECTA,
  PREVIA_ORDENAR,
  PREVIA_TEXTO_A_MEMORIZAR,
  TEXTOS_ELEMENTO,
} from "./mensajes"

type VistaPreviaEjercicioProps = {
  categoria: CategoriaEjercicio | undefined
  formato: FormatoEjercicio | undefined
  consigna: string
  textoAMemorizar: string
  opciones: OpcionFormulario[]
}

/**
 * Orden "desordenado" fijo para la vista previa de ordenar: primero las posiciones pares y
 * después las impares (por ejemplo 2, 4, 1, 3, 5). Así no cambia mientras se escribe.
 * Si las posiciones todavía no son válidas, se muestran como están cargados.
 */
function desordenar(opciones: OpcionFormulario[]) {
  const posiciones = opciones.map((o) => Number(o.posicion))
  const validas =
    posiciones.every((p) => Number.isInteger(p) && p >= 1 && p <= opciones.length) &&
    new Set(posiciones).size === opciones.length
  if (!validas) return opciones
  const enOrden = [...opciones].sort((a, b) => Number(a.posicion) - Number(b.posicion))
  return [...enOrden.filter((_, i) => i % 2 === 1), ...enOrden.filter((_, i) => i % 2 === 0)]
}

/**
 * "Así lo ve la persona" (Figma 52:1816): etiqueta de categoría, la consigna en h3 y las
 * opciones como "Respuesta de juego" (borde input de 2 px; la correcta en success-soft con
 * borde success y tilde). En MEMORIA se muestra antes el texto a memorizar y en ORDENAR los
 * pasos desordenados. Las opciones son de muestra: no se pueden tocar.
 */
export function VistaPreviaEjercicio({
  categoria,
  formato,
  consigna,
  textoAMemorizar,
  opciones,
}: VistaPreviaEjercicioProps) {
  const id = useId()
  const ordenar = formato === "ORDENAR"
  const aMostrar = ordenar ? desordenar(opciones) : opciones

  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <h2 id={id} className="font-sans text-body font-bold text-foreground">
        {ADMIN_VISTA_PREVIA}
      </h2>
      <Card className="items-stretch gap-3">
        {categoria ? (
          <div>
            <EtiquetaCategoriaEjercicio categoria={categoria} />
          </div>
        ) : null}
        <h3 className="wrap-break-word">
          {consigna.trim() || PREVIA_CONSIGNA}
        </h3>
        {categoria === "MEMORIA" ? (
          <p className="rounded-md bg-secondary p-4 whitespace-pre-line wrap-break-word contraste-alto:border-2 contraste-alto:border-foreground">
            {textoAMemorizar.trim() || PREVIA_TEXTO_A_MEMORIZAR}
          </p>
        ) : null}
        {ordenar ? <p className="text-muted-foreground">{PREVIA_ORDENAR}</p> : null}
        <ul className="flex flex-col gap-3">
          {aMostrar.map((opcion, i) => {
            const correcta = !ordenar && opcion.esCorrecta
            return (
              <li
                key={i}
                className={`flex min-h-(--altura-campo) items-center gap-3 rounded-md border-2 px-4 py-3 ${
                  correcta ? "border-success bg-success-soft" : "border-input bg-card"
                }`}
              >
                {opcion.imagen ? (
                  <img
                    src={opcion.imagen}
                    alt=""
                    className="size-12 shrink-0 rounded-sm object-cover"
                  />
                ) : null}
                <span className="min-w-0 flex-1 wrap-break-word">
                  {opcion.texto.trim() || `${(ordenar ? TEXTOS_ELEMENTO.paso : TEXTOS_ELEMENTO.opcion).mayuscula} ${i + 1}`}
                </span>
                {correcta ? (
                  <>
                    <HugeiconsIcon
                      icon={Tick02Icon}
                      strokeWidth={2}
                      className="size-6 shrink-0 text-success"
                      aria-hidden="true"
                    />
                    <span className="sr-only">{PREVIA_CORRECTA}</span>
                  </>
                ) : null}
              </li>
            )
          })}
        </ul>
      </Card>
    </section>
  )
}
