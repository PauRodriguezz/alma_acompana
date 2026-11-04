import { useEffect, useId, useRef } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Alert02Icon, CheckmarkCircle02Icon } from "@hugeicons/core-free-icons"

import { Button } from "@/components/ui/button"

import {
  BOTON_CONTINUAR,
  RESPUESTA_ERA,
  respuestaEra,
  RESULTADO_ACIERTO,
  RESULTADO_ERROR,
} from "../mensajes"
import type { ResultadoRespuesta } from "../tipos"

/**
 * Panel de respuesta (Figma 134:5064 y 134:5074): "¡Muy bien!" o "¡Casi!" con ícono (nunca solo
 * color), "La respuesta era…" (en ordenar, la lista numerada), la explicación si hay y "Continuar".
 * Va en el flujo, debajo de las opciones (no fijo al pie): al aparecer se lleva a la vista y recibe
 * el foco. El anuncio para el lector de pantalla lo hace la región role="status" del ejercicio.
 */
export function PanelRespuesta({
  resultado,
  onContinuar,
}: {
  resultado: ResultadoRespuesta
  onContinuar: () => void
}) {
  const ref = useRef<HTMLElement>(null)
  const idTitulo = useId()
  const { acierto, respuestaCorrecta, explicacion } = resultado

  useEffect(() => {
    const panel = ref.current
    if (!panel) return
    // Si el panel no entra en la pantalla (letra grande), se muestra desde arriba; si entra, se
    // mueve lo justo para que se vea entero.
    const entra = panel.getBoundingClientRect().height < window.innerHeight * 0.8
    panel.scrollIntoView({ block: entra ? "nearest" : "start" })
    panel.focus({ preventScroll: true })
  }, [])

  return (
    <section
      ref={ref}
      tabIndex={-1}
      aria-labelledby={idTitulo}
      className={`flex flex-col gap-3 rounded-xl p-5 outline-none scroll-mt-4 max-lg:scroll-mb-[calc(var(--nav-inferior)+1rem)] contraste-alto:border-2 contraste-alto:bg-card ${
        acierto
          ? "bg-success-soft text-success contraste-alto:border-success"
          : "bg-warning-soft text-warning contraste-alto:border-warning"
      }`}
    >
      <h2 id={idTitulo} className="flex items-center gap-2 text-h3 text-current">
        <HugeiconsIcon
          icon={acierto ? CheckmarkCircle02Icon : Alert02Icon}
          className="size-6 shrink-0"
          aria-hidden="true"
        />
        {acierto ? RESULTADO_ACIERTO : RESULTADO_ERROR}
      </h2>

      {respuestaCorrecta.length === 1 ? (
        <p>{respuestaEra(respuestaCorrecta[0])}</p>
      ) : (
        <div>
          <p>{RESPUESTA_ERA}</p>
          <ol className="list-decimal pl-7">
            {respuestaCorrecta.map((paso, i) => (
              <li key={i}>{paso}</li>
            ))}
          </ol>
        </div>
      )}

      {explicacion ? <p>{explicacion}</p> : null}

      <Button className="w-full" onClick={onContinuar}>
        {BOTON_CONTINUAR}
      </Button>
    </section>
  )
}
