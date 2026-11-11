import type { CSSProperties, ReactNode } from "react"

import { cn } from "@/lib/utils"

import { NOMBRE_DEL_CAMINO } from "../mensajes"
import type { RecuerdoResumen } from "../tipos"
import { ParadaDeRecuerdo } from "./ParadaDeRecuerdo"

/** Un recuerdo del camino con su número (la posición cronológica entre todos los que tienen año). */
export type ParadaNumerada = { numero: number; recuerdo: RecuerdoResumen }

const COLUMNAS = 3

type Props = {
  paradas: ParadaNumerada[]
  /** La parada "?" al final del camino: la tarjeta de la próxima pregunta, si hay una pendiente. */
  proximaParada?: ReactNode
}

/**
 * El camino de paradas (Figma, contrato 2.6). Es una lista ordenada (el orden es el cronológico).
 * - Celular: una parada por fila, con el número a un costado alternado.
 * - Escritorio (desde `lg`): filas de 3 paradas en serpiente: la fila 1 va de izquierda a derecha, la
 *   2 de derecha a izquierda y así. La parada "?" ocupa el lugar que sigue a la última.
 * La banda blanca, la línea punteada y los números son decorativos (`aria-hidden`).
 */
export function CaminoDeRecuerdos({ paradas, proximaParada }: Props) {
  const total = paradas.length + (proximaParada ? 1 : 0)
  if (total === 0) return null

  return (
    <ol
      aria-label={NOMBRE_DEL_CAMINO}
      className="flex flex-col gap-4 rounded-xl bg-secondary p-4 lg:grid lg:grid-cols-3 lg:gap-x-0 lg:gap-y-4 lg:p-6"
    >
      {paradas.map((parada, i) => (
        <Parada key={parada.recuerdo.id} indice={i} total={total} etiqueta={String(parada.numero)}>
          <ParadaDeRecuerdo recuerdo={parada.recuerdo} />
        </Parada>
      ))}
      {proximaParada ? (
        <Parada indice={paradas.length} total={total} etiqueta="?" proxima>
          {proximaParada}
        </Parada>
      ) : null}
    </ol>
  )
}

function Parada({
  indice,
  total,
  etiqueta,
  proxima = false,
  children,
}: {
  indice: number
  total: number
  etiqueta: string
  proxima?: boolean
  children: ReactNode
}) {
  // Posición en la serpiente de escritorio: las filas impares (la 2.ª, la 4.ª…) van al revés.
  const fila = Math.floor(indice / COLUMNAS)
  const orden = indice % COLUMNAS
  const columna = fila % 2 === 0 ? orden : COLUMNAS - 1 - orden
  const esUltima = indice === total - 1
  const redondeaIzquierda = columna === 0 || (esUltima && fila % 2 === 1)
  const redondeaDerecha = columna === COLUMNAS - 1 || (esUltima && fila % 2 === 0)

  const posicion = { "--col": columna + 1, "--fila": fila + 1 } as CSSProperties
  const alternada = indice % 2 === 0

  return (
    <li
      style={posicion}
      className="flex flex-col gap-2 lg:[grid-column-start:var(--col)] lg:[grid-row-start:var(--fila)]"
    >
      <div
        aria-hidden="true"
        className={cn(
          "relative flex h-[calc(52rem/17)] items-center rounded-full bg-card",
          redondeaIzquierda ? "lg:rounded-l-full" : "lg:rounded-l-none",
          redondeaDerecha ? "lg:rounded-r-full" : "lg:rounded-r-none"
        )}
      >
        <span
          className={cn(
            "mx-5 block h-0 flex-1 border-t-2 border-dashed border-accent",
            !redondeaIzquierda && "lg:ml-0",
            !redondeaDerecha && "lg:mr-0"
          )}
        />
        <span
          className={cn(
            "absolute top-1/2 grid size-7 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full text-caption font-semibold",
            "lg:left-1/2",
            alternada ? "max-lg:left-[22%]" : "max-lg:left-[62%]",
            proxima
              ? "border-2 border-dashed border-accent bg-card text-accent"
              : "bg-accent text-accent-foreground"
          )}
        >
          {etiqueta}
        </span>
      </div>
      <div className={cn("lg:mx-2", alternada ? "max-lg:mr-10" : "max-lg:ml-10")}>{children}</div>
    </li>
  )
}
