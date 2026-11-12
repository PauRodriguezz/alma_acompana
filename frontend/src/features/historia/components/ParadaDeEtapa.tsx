import type { CSSProperties } from "react"

import { cn } from "@/lib/utils"

import { resumenDeTarjeta, type EtapaConRecuerdos } from "../agruparPorEtapa"
import { variablesDeEtapa } from "../etapas"
import { ETAPA_SIN_RECUERDOS, masRecuerdos, nombreDeParada } from "../mensajes"
import { posicionDeParada } from "../posiciones"

/** Unidades de diseño (ancho 360, contrato Anexo A). `--u` la define el mapa: 1/360 del ancho disponible. */
const u = (n: number) => `calc(${n} * var(--u))`

type Props = {
  grupo: EtapaConRecuerdos
  indice: number
  elegida: boolean
  /** Mientras la persona no tocó ninguna bandera, el punto late. */
  conAviso: boolean
  onElegir: () => void
}

/**
 * Una parada: el punto, el mástil, el banderín y la tarjeta. Todo es un único botón (contrato, 2.1).
 * Los nombres y los títulos son texto real, con tamaño en rem; solo las formas son dibujo.
 */
export function ParadaDeEtapa({ grupo, indice, elegida, conAviso, onElegir }: Props) {
  const { etapa, recuerdos } = grupo
  const { y, lado } = posicionDeParada(indice)
  const derecha = lado === "der"
  const vacia = recuerdos.length === 0
  const { titulos, masDe } = resumenDeTarjeta(recuerdos)

  // El botón va desde el borde exterior del punto hasta el borde de la tarjeta.
  const cajaX = derecha ? 87 : 28
  const style: CSSProperties = {
    ...(variablesDeEtapa(etapa) as CSSProperties),
    left: u(cajaX),
    top: u(y - 38),
    width: u(245),
    minHeight: u(78),
  }
  const anchoBanderin = etapa.id === "OTROS" ? 138 : 116
  const muesca = u(12)

  return (
    <button
      type="button"
      data-etapa={etapa.id}
      aria-label={nombreDeParada(etapa.nombre, recuerdos.length)}
      aria-pressed={elegida}
      onClick={onElegir}
      className="absolute block rounded-lg text-left"
      style={style}
    >
      {/* Punto de parada, sobre el camino */}
      <span
        aria-hidden="true"
        className={cn(
          "absolute box-border rounded-full border-3 border-(--etapa-fuerte) bg-white contraste-alto:border-foreground contraste-alto:bg-background",
          conAviso && "mapa-latido",
          derecha ? "left-0" : "right-0"
        )}
        style={{ top: u(29), width: u(18), height: u(18) }}
      />
      {/* Mástil con su bolita */}
      <span
        aria-hidden="true"
        className="absolute top-0 w-[2.5px] rounded-full bg-[#3F4F5C] contraste-alto:bg-foreground"
        style={{ left: derecha ? u(69) : u(176), height: u(78) }}
      >
        <span className="absolute -top-px left-1/2 size-1.5 -translate-x-1/2 rounded-full bg-[#3F4F5C] contraste-alto:bg-foreground" />
      </span>

      <span
        className={cn("flex flex-col", derecha ? "" : "items-end")}
        style={{ marginLeft: derecha ? u(69) : 0, width: u(176), paddingTop: u(4) }}
      >
        <span
          className="flex h-[1.5rem] w-fit items-center bg-(--etapa-fuerte) font-heading text-[0.8125rem] font-medium whitespace-nowrap text-white contraste-alto:bg-foreground contraste-alto:text-background"
          style={{
            minWidth: u(anchoBanderin),
            paddingLeft: derecha ? "0.5rem" : `calc(${muesca} + 0.25rem)`,
            paddingRight: derecha ? `calc(${muesca} + 0.25rem)` : "0.5rem",
            clipPath: derecha
              ? `polygon(0 0, 100% 0, calc(100% - ${muesca}) 50%, 100% 100%, 0 100%)`
              : `polygon(0 0, 100% 0, 100% 100%, 0 100%, ${muesca} 50%)`,
          }}
        >
          {etapa.nombre}
        </span>
        <span
          className={cn(
            "mt-[0.4rem] box-border flex w-full flex-col rounded-lg bg-white px-2 py-1 text-[0.875rem] leading-[1.25] text-foreground contraste-alto:bg-background",
            "border-[1.5px] border-(--etapa-claro) contraste-alto:border-2 contraste-alto:border-foreground",
            elegida && "border-3 border-(--etapa-fuerte) contraste-alto:border-4 contraste-alto:border-foreground",
            vacia && "border-dashed"
          )}
        >
          {vacia ? (
            <span className="text-muted-foreground">{ETAPA_SIN_RECUERDOS}</span>
          ) : (
            <>
              {titulos.map((titulo, i) => (
                <span key={i} className="truncate">
                  {titulo}
                </span>
              ))}
              {masDe > 0 ? (
                <span className="truncate font-semibold text-(--etapa-fuerte) contraste-alto:text-foreground">
                  {masRecuerdos(masDe)}
                </span>
              ) : null}
            </>
          )}
        </span>
      </span>
    </button>
  )
}
