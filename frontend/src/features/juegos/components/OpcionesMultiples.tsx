import { HugeiconsIcon } from "@hugeicons/react"
import { InformationCircleIcon, Tick02Icon } from "@hugeicons/core-free-icons"

import { RESPUESTA_CORRECTA, TU_RESPUESTA } from "../mensajes"
import type { OpcionJuego, ResultadoRespuesta } from "../tipos"

/**
 * Opciones de OPCION_MULTIPLE (Figma 30:464, 134:5064 y 134:5074). Tocar una opción ES la
 * respuesta (contrato, 10.2). Mientras viaja y después del resultado, las opciones quedan con
 * aria-disabled (no disabled: así el foco no se pierde). Con el resultado, la correcta (su texto es
 * respuestaCorrecta[0]) lleva tilde y color de acierto, y la elegida equivocada, ícono y "Tu respuesta".
 */
export function OpcionesMultiples({
  opciones,
  elegidaId,
  resultado,
  bloqueadas,
  onElegir,
}: {
  opciones: OpcionJuego[]
  elegidaId: number | null
  resultado: ResultadoRespuesta | undefined
  bloqueadas: boolean
  onElegir: (id: number) => void
}) {
  const textoCorrecto = resultado?.respuestaCorrecta[0]

  return (
    <ul className="flex flex-col gap-4">
      {opciones.map((opcion) => {
        const elegida = opcion.id === elegidaId
        const correcta = resultado !== undefined && opcion.texto === textoCorrecto
        const equivocada = resultado !== undefined && elegida && !correcta

        let estado = "border-input bg-card"
        if (correcta) estado = "border-success bg-success-soft contraste-alto:bg-card"
        else if (elegida) estado = "border-heading bg-secondary contraste-alto:bg-card"

        return (
          <li key={opcion.id}>
            <button
              type="button"
              aria-disabled={bloqueadas}
              onClick={() => {
                if (!bloqueadas) onElegir(opcion.id)
              }}
              className={`flex min-h-(--altura-campo) w-full items-center gap-3 rounded-md border-2 px-4 py-3 text-left text-foreground transition-colors aria-disabled:cursor-default contraste-alto:border-foreground ${
                bloqueadas ? "" : "cursor-pointer hover:bg-outline-hover"
              } ${estado}`}
            >
              {opcion.imagen ? (
                <img
                  src={opcion.imagen}
                  alt=""
                  className="size-[calc(64rem/17)] shrink-0 rounded-sm object-cover"
                />
              ) : null}
              {/* "Tu respuesta" va debajo del texto: al costado, con letra grande, lo aplastaba */}
              <span className="flex min-w-0 flex-1 flex-col gap-1">
                <span className="wrap-break-word">{opcion.texto}</span>
                {equivocada ? (
                  <span className="flex items-center gap-1 text-caption font-semibold text-heading">
                    <HugeiconsIcon
                      icon={InformationCircleIcon}
                      className="size-6 shrink-0"
                      aria-hidden="true"
                    />
                    {TU_RESPUESTA}
                  </span>
                ) : null}
              </span>
              {correcta ? (
                <>
                  <HugeiconsIcon
                    icon={Tick02Icon}
                    strokeWidth={2}
                    className="size-6 shrink-0 text-success"
                    aria-hidden="true"
                  />
                  <span className="sr-only">{RESPUESTA_CORRECTA}</span>
                </>
              ) : null}
            </button>
          </li>
        )
      })}
    </ul>
  )
}
