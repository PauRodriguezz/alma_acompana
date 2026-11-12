import type { CSSProperties } from "react"

import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import { variablesDeEtapa, type IdEtapa } from "../etapas"
import { NOMBRE_DEL_MAPA } from "../mensajes"
import "../mapa.css"
import { CirculoDeEtapa } from "./IlustracionDeEtapa"
import { posicionDeParada } from "../posiciones"
import { ParadaDeEtapa } from "./ParadaDeEtapa"

// Camino del contrato (Anexo A), cortado en la última parada que se dibuja.
const TRAMOS = [
  "M96 70",
  "C96 130 264 130 264 190",
  "C264 250 96 250 96 310",
  "C96 370 264 370 264 430",
  "C264 490 96 490 96 550",
  "C96 610 264 610 264 670",
]
const ARBOLITOS = [
  [300, 128],
  [60, 248],
  [300, 368],
  [60, 488],
  [300, 608],
]

type Props = {
  etapas: EtapaConRecuerdos[]
  elegida: IdEtapa | null
  /** Todavía no se tocó ninguna bandera: los puntos laten. */
  conAviso: boolean
  onElegir: (id: IdEtapa) => void
}

/**
 * El mapa (contrato, 2.1): un camino que serpentea con una bandera por etapa. El dibujo (camino, arbolitos,
 * ilustraciones) es decorativo y los lectores de pantalla lo ignoran; las paradas son botones con texto real.
 * Todo se ubica en unidades de diseño de un ancho de 360, escaladas al ancho disponible con `--u`.
 */
export function MapaDeVida({ etapas, elegida, conAviso, onElegir }: Props) {
  const ultimaY = posicionDeParada(etapas.length - 1).y
  const alto = ultimaY + 80
  const camino = TRAMOS.slice(0, etapas.length).join(" ")

  return (
    <div className="mx-auto w-full max-w-[28rem] [container-type:inline-size]">
      <div
        role="group"
        aria-label={NOMBRE_DEL_MAPA}
        className="relative"
        style={{ "--u": "calc(100cqw / 360)", height: `calc(${alto} * var(--u))` } as CSSProperties}
      >
        <svg
          viewBox={`0 0 360 ${alto}`}
          aria-hidden="true"
          focusable="false"
          className="absolute inset-0 size-full"
        >
          {etapas.length > 1 ? (
            <>
              <path
                d={camino}
                fill="none"
                strokeWidth="28"
                strokeLinecap="round"
                className="stroke-[#F4C0D1] contraste-alto:stroke-muted-foreground"
              />
              <path
                d={camino}
                fill="none"
                strokeWidth="2"
                strokeDasharray="3 9"
                strokeLinecap="round"
                className="stroke-[#FFFBF4] contraste-alto:stroke-background"
              />
            </>
          ) : null}
          <g className="contraste-alto:hidden">
            {ARBOLITOS.slice(0, Math.max(0, etapas.length - 1)).map(([x, y]) => (
              <g key={`${x}-${y}`} transform={`translate(${x} ${y})`}>
                <rect x="-2" y="8" width="4" height="9" fill="#854F0B" opacity=".7" />
                <circle r="12" fill="#97C459" />
                <circle cx="-3" cy="-3" r="6" fill="#C0DD97" />
              </g>
            ))}
          </g>
        </svg>

        {etapas.map((grupo, i) => {
          const { y, xIlustracion } = posicionDeParada(i)
          return (
            <span
              key={`ilus-${grupo.etapa.id}`}
              className="absolute"
              style={
                {
                  ...variablesDeEtapa(grupo.etapa),
                  left: `calc(${xIlustracion - 26} * var(--u))`,
                  top: `calc(${y - 26} * var(--u))`,
                  width: "calc(52 * var(--u))",
                  height: "calc(52 * var(--u))",
                } as CSSProperties
              }
            >
              <CirculoDeEtapa id={grupo.etapa.id} elegida={elegida === grupo.etapa.id} className="size-full" />
            </span>
          )
        })}

        {etapas.map((grupo, i) => (
          <ParadaDeEtapa
            key={grupo.etapa.id}
            grupo={grupo}
            indice={i}
            elegida={elegida === grupo.etapa.id}
            conAviso={conAviso}
            onElegir={() => onElegir(grupo.etapa.id)}
          />
        ))}
      </div>
    </div>
  )
}
