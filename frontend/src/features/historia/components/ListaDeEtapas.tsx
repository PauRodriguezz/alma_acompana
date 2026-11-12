import type { CSSProperties } from "react"

import { cn } from "@/lib/utils"

import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import { variablesDeEtapa, type IdEtapa } from "../etapas"
import { cantidadDeRecuerdos, NOMBRE_DEL_MAPA, nombreDeParada } from "../mensajes"
import { CirculoDeEtapa } from "./IlustracionDeEtapa"

type Props = {
  etapas: EtapaConRecuerdos[]
  elegida: IdEtapa | null
  onElegir: (id: IdEtapa) => void
}

/**
 * El mapa con letra Grande o Muy grande (contrato, 2.4): una lista vertical de etapas. Cada tarjeta tiene su
 * ilustración, el banderín con el nombre, la cantidad y hasta dos títulos completos. Una línea de camino decorativa
 * une las ilustraciones por un costado.
 */
export function ListaDeEtapas({ etapas, elegida, onElegir }: Props) {
  return (
    <ul aria-label={NOMBRE_DEL_MAPA} className="relative m-0 flex list-none flex-col gap-4 p-0">
      <li
        aria-hidden="true"
        className="absolute top-6 bottom-6 left-[1.875rem] w-2 -translate-x-1/2 rounded-full bg-[#F4C0D1] contraste-alto:bg-muted-foreground"
      />
      {etapas.map(({ etapa, recuerdos }) => {
        const esta = elegida === etapa.id
        return (
          <li key={etapa.id} className="relative flex items-start gap-3" style={variablesDeEtapa(etapa) as CSSProperties}>
            <CirculoDeEtapa id={etapa.id} elegida={esta} className="size-[3.75rem]" />
            <button
              type="button"
              data-etapa={etapa.id}
              aria-label={nombreDeParada(etapa.nombre, recuerdos.length)}
              aria-pressed={esta}
              onClick={() => onElegir(etapa.id)}
              className={cn(
                "box-border flex min-h-[2.75rem] min-w-0 flex-1 flex-col items-start gap-2 rounded-lg bg-white p-3 text-left",
                "border-[1.5px] border-(--etapa-claro) contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-background",
                esta && "border-3 border-(--etapa-fuerte) contraste-alto:border-4 contraste-alto:border-foreground",
                recuerdos.length === 0 && "border-dashed"
              )}
            >
              <span
                className="rounded-sm bg-(--etapa-fuerte) px-3 py-1 font-heading font-medium text-white contraste-alto:bg-foreground contraste-alto:text-background"
              >
                {etapa.nombre}
              </span>
              <span className="font-semibold text-(--etapa-fuerte) contraste-alto:text-foreground">
                {cantidadDeRecuerdos(recuerdos.length)}
              </span>
              {recuerdos.slice(0, 2).map((r) => (
                <span key={r.id} className="wrap-break-word text-foreground">
                  {r.titulo}
                </span>
              ))}
            </button>
          </li>
        )
      })}
    </ul>
  )
}
