import { HugeiconsIcon } from "@hugeicons/react"
import { ArrowLeft01Icon, ArrowRight01Icon, Calendar03Icon } from "@hugeicons/core-free-icons"
import { eachDayOfInterval, endOfWeek, format, isSameDay, startOfWeek } from "date-fns"
import { es } from "date-fns/locale"

import { Button } from "@/components/ui/button"
import { AspectoDia } from "@/components/ui/dia-calendario"
import { nombreDelDia } from "@/lib/fechas"

import {
  NOMBRE_DIA_CON_CITA,
  NOMBRE_TIRA_SEMANA,
  SEMANA_ANTERIOR,
  SEMANA_SIGUIENTE,
  VER_EL_MES,
} from "../mensajes"

const FLECHA =
  "inline-flex size-(--altura-boton) shrink-0 items-center justify-center rounded-md text-heading hover:bg-secondary"

type Props = {
  /** El día elegido: su semana (de lunes a domingo) es la que se muestra. */
  dia: Date
  hoy: Date
  /** Días de la semana con una cita (llevan un punto). */
  diasConCita: Date[]
  onElegir: (dia: Date) => void
  onSemana: (paso: -1 | 1) => void
  onVerMes: () => void
}

/**
 * Tira de una semana (celular): siete botones propios, uno por día, con el elegido resaltado.
 * No se arma con el Calendar de ui porque ese muestra siempre el mes entero. Cada día es un
 * botón de al menos 44 px con la fecha completa como nombre accesible ("tiene una cita" si
 * corresponde), `aria-pressed` si está elegido y `aria-current="date"` si es hoy.
 */
export function TiraSemana({ dia, hoy, diasConCita, onElegir, onSemana, onVerMes }: Props) {
  const lunes = startOfWeek(dia, { weekStartsOn: 1 })
  const domingo = endOfWeek(dia, { weekStartsOn: 1 })
  const dias = eachDayOfInterval({ start: lunes, end: domingo })

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-2 px-(--page-x)">
        <button type="button" className={FLECHA} aria-label={SEMANA_ANTERIOR} onClick={() => onSemana(-1)}>
          <HugeiconsIcon icon={ArrowLeft01Icon} className="size-6" aria-hidden="true" />
        </button>
        <p className="text-center font-heading text-h3 font-medium text-heading">
          {format(lunes, "d MMM", { locale: es })} – {format(domingo, "d MMM", { locale: es })}
        </p>
        <button type="button" className={FLECHA} aria-label={SEMANA_SIGUIENTE} onClick={() => onSemana(1)}>
          <HugeiconsIcon icon={ArrowRight01Icon} className="size-6" aria-hidden="true" />
        </button>
      </div>

      <div role="group" aria-label={NOMBRE_TIRA_SEMANA} className="grid grid-cols-7">
        {dias.map((fecha) => {
          const esHoy = isSameDay(fecha, hoy)
          const seleccionado = isSameDay(fecha, dia)
          const marcado = diasConCita.some((conCita) => isSameDay(conCita, fecha))
          return (
            <button
              key={fecha.getTime()}
              type="button"
              data-dia={format(fecha, "yyyy-MM-dd")}
              aria-pressed={seleccionado}
              aria-current={esHoy ? "date" : undefined}
              aria-label={nombreDelDia(fecha, { today: esHoy, marcado }, NOMBRE_DIA_CON_CITA)}
              onClick={() => onElegir(fecha)}
              className="flex min-w-[44px] flex-col items-center gap-1 py-1"
            >
              <span
                aria-hidden="true"
                className="text-caption font-semibold tracking-tighter text-muted-foreground"
              >
                {format(fecha, "EEE", { locale: es })}
              </span>
              <span className="relative flex h-[calc(48rem/17)] w-full items-center justify-center">
                <AspectoDia esHoy={esHoy} seleccionado={seleccionado} marcado={marcado}>
                  {format(fecha, "d")}
                </AspectoDia>
              </span>
            </button>
          )
        })}
      </div>

      <div className="px-(--page-x)">
        <Button variant="outline" className="w-full" onClick={onVerMes}>
          <HugeiconsIcon icon={Calendar03Icon} className="size-6" aria-hidden="true" />
          {VER_EL_MES}
        </Button>
      </div>
    </div>
  )
}
