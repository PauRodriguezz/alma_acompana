import { cn } from "@/lib/utils"

import type { IdEtapa } from "../etapas"

const RAYOS = Array.from({ length: 8 }, (_, i) => (i * Math.PI) / 4)

/**
 * Ilustración de la etapa (contrato, Anexo A): decorativa, una por etapa. Se dibuja con `currentColor`
 * y el color de los recortes sale de `--ilus-fondo`, que pone quien la usa (así cambia con el alto contraste).
 */
export function IlustracionDeEtapa({ id }: { id: IdEtapa }) {
  return (
    <svg viewBox="-26 -26 52 52" aria-hidden="true" focusable="false" className="size-full">
      {id === "INFANCIA" ? (
        <>
          <path d="M0 -14 L10 -3 L0 8 L-10 -3 Z" fill="currentColor" />
          <path d="M0 -14 V8 M-10 -3 H10" stroke="var(--ilus-fondo)" strokeWidth="1.2" fill="none" />
          <path d="M0 8 C-3 14 6 14 3 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        </>
      ) : null}
      {id === "ADOLESCENCIA" ? (
        <>
          <path d="M-14 -8 L0 -4 V11 L-14 7 Z" fill="currentColor" />
          <path d="M14 -8 L0 -4 V11 L14 7 Z" fill="currentColor" opacity=".7" />
        </>
      ) : null}
      {id === "JUVENTUD" ? <path d="M0 12 C-18 -2 -8 -16 0 -6 C8 -16 18 -2 0 12 Z" fill="currentColor" /> : null}
      {id === "ADULTEZ" ? (
        <>
          <path d="M-14 -1 L0 -14 L14 -1 Z" fill="currentColor" />
          <rect x="-10" y="-1" width="20" height="14" fill="currentColor" opacity=".8" />
          <rect x="-3" y="4" width="6" height="9" fill="var(--ilus-fondo)" />
        </>
      ) : null}
      {id === "VEJEZ" ? (
        <>
          <circle cx="0" cy="0" r="7" fill="currentColor" />
          {RAYOS.map((a) => (
            <line
              key={a}
              x1={(11 * Math.cos(a)).toFixed(1)}
              y1={(11 * Math.sin(a)).toFixed(1)}
              x2={(15 * Math.cos(a)).toFixed(1)}
              y2={(15 * Math.sin(a)).toFixed(1)}
              stroke="currentColor"
              strokeWidth="2.2"
              strokeLinecap="round"
            />
          ))}
        </>
      ) : null}
      {id === "OTROS" ? (
        <path d="M0 -14 L4 -4 L14 0 L4 4 L0 14 L-4 4 L-14 0 L-4 -4 Z" fill="currentColor" />
      ) : null}
    </svg>
  )
}

/**
 * El círculo de la ilustración. Usa las variables de la etapa (`variablesDeEtapa`) que pone un ancestro.
 * En alto contraste: fondo blanco, borde negro de 2 px (4 px si es la etapa elegida) e ilustración negra.
 */
export function CirculoDeEtapa({
  id,
  elegida = false,
  className,
}: {
  id: IdEtapa
  elegida?: boolean
  className?: string
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "flex shrink-0 items-center justify-center rounded-full bg-(--etapa-claro) text-(--etapa-fuerte) [--ilus-fondo:var(--etapa-claro)]",
        "contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-background contraste-alto:text-foreground contraste-alto:[--ilus-fondo:var(--background)]",
        elegida && "contraste-alto:border-4",
        className
      )}
    >
      <span className="size-[64%]">
        <IlustracionDeEtapa id={id} />
      </span>
    </span>
  )
}
