import { useEffect, useMemo, useRef, useState } from "react"
import {
  addDays,
  endOfMonth,
  endOfWeek,
  format,
  parseISO,
  startOfDay,
  startOfMonth,
  startOfWeek,
} from "date-fns"
import { es } from "date-fns/locale"
import { cn } from "@/lib/utils"

import { hoyArgentina, mayuscula, nombreDelDia } from "@/lib/fechas"
import { Alert } from "@/components/ui/alert"
import { Calendar } from "@/components/ui/calendar"
import { mensajeDeError } from "@/lib/errores"
import { useAhora } from "@/lib/useAhora"
import { ESTADO_CARGANDO } from "@/lib/mensajes"

import { fechaCompleta } from "../fechas"
import {
  anuncioMes,
  anuncioSemana,
  LEYENDA_PUNTO,
  NOMBRE_DIA_CON_CITA,
  SIN_AVISOS_DEL_DIA,
  tituloDelDia,
} from "../mensajes"
import { useCambioDeEstado, useAgenda } from "../useRecordatorios"

import { LineaDeTiempoDelDia } from "./LineaDeTiempoDelDia"
import { TiraSemana } from "./TiraSemana"

type Modo = "mes" | "semana"

const FORMATO = "yyyy-MM-dd"
// El mismo corte que `lg` de Tailwind: no depende del tamaño de letra (rem de media query).
const ESCRITORIO = "(min-width: 64rem)"

// En celular el calendario ocupa todo el ancho de la pantalla (sin los márgenes de la página):
// con letra "Muy grande" esos márgenes crecen y siete columnas de 44 px ya no entrarían.
const TARJETA =
  "bg-card py-4 [box-shadow:var(--sombra-sm)] contraste-alto:border-y-2 contraste-alto:border-foreground max-lg:-mx-(--page-x) lg:mx-auto lg:max-w-[calc(560rem/17)] lg:rounded-lg lg:px-4 lg:contraste-alto:border-2"

/**
 * Calendario (Figma): el mes con un punto en los días con cita y, debajo, el detalle del día
 * elegido en línea de tiempo.
 * - Celular: al tocar un día, el mes se pliega a la tira de esa semana (con "Ver el mes" para
 *   volver). El foco pasa al día elegido y el cambio se anuncia.
 * - Escritorio: el mes se queda siempre a la vista.
 * Las medicaciones solo se marcan como tomadas en el día de hoy.
 */
export function CalendarioRecordatorios() {
  const ahora = useAhora()
  const hoy = parseISO(format(ahora, FORMATO))
  // "Hoy" sale de la hora de Argentina y no de la del dispositivo
  const [dia, setDia] = useState<Date>(() => parseISO(hoyArgentina()))
  const [mes, setMes] = useState<Date>(() => startOfMonth(parseISO(hoyArgentina())))
  const [modo, setModo] = useState<Modo>("mes")
  const [anuncio, setAnuncio] = useState("")
  const mesRef = useRef<HTMLDivElement>(null)
  const tiraRef = useRef<HTMLDivElement>(null)
  // Solo se mueve el foco cuando el cambio lo provocó la persona, no al cargar la pantalla.
  const pedirFoco = useRef(false)

  const diaTexto = format(dia, FORMATO)
  const esHoy = diaTexto === format(hoy, FORMATO)

  // Puntos de cita: los del mes visible y, ya plegado, los de la semana (puede cruzar de mes).
  // Con el mes a la vista las dos consultas son la misma.
  const citasDelMes = useAgenda(
    format(startOfMonth(mes), FORMATO),
    format(endOfMonth(mes), FORMATO),
    "CITA"
  )
  const citasDeLaSemana = useAgenda(
    format(modo === "semana" ? startOfWeek(dia, { weekStartsOn: 1 }) : startOfMonth(mes), FORMATO),
    format(modo === "semana" ? endOfWeek(dia, { weekStartsOn: 1 }) : endOfMonth(mes), FORMATO),
    "CITA"
  )
  const diasConCita = useMemo(
    () =>
      [...(citasDelMes.data ?? []), ...(citasDeLaSemana.data ?? [])].map((item) =>
        startOfDay(parseISO(item.fechaHoraProgramada))
      ),
    [citasDelMes.data, citasDeLaSemana.data]
  )

  const delDia = useAgenda(diaTexto, diaTexto)
  const { cambiar, ocupadoId, anuncio: anuncioEstado, error: errorEstado } = useCambioDeEstado()

  useEffect(() => {
    if (!pedirFoco.current) return
    pedirFoco.current = false
    const raiz = modo === "semana" ? tiraRef.current : mesRef.current
    raiz?.querySelector<HTMLElement>(`[data-dia="${diaTexto}"]`)?.focus()
  }, [modo, diaTexto])

  function textoSemana(desdeDia: Date) {
    const lunes = startOfWeek(desdeDia, { weekStartsOn: 1 })
    const domingo = endOfWeek(desdeDia, { weekStartsOn: 1 })
    return anuncioSemana(
      format(lunes, "d 'de' MMMM", { locale: es }),
      format(domingo, "d 'de' MMMM", { locale: es }),
      fechaCompleta(format(desdeDia, FORMATO))
    )
  }

  function tocarDiaDelMes(elegido: Date) {
    setDia(elegido)
    // En escritorio el mes se queda; en celular se pliega a la semana.
    if (window.matchMedia(ESCRITORIO).matches) return
    pedirFoco.current = true
    setModo("semana")
    setAnuncio(textoSemana(elegido))
  }

  function moverSemana(paso: -1 | 1) {
    const nuevo = addDays(dia, 7 * paso)
    setDia(nuevo)
    setMes(startOfMonth(nuevo))
    setAnuncio(textoSemana(nuevo))
  }

  function verElMes() {
    pedirFoco.current = true
    setMes(startOfMonth(dia))
    setModo("mes")
    setAnuncio(anuncioMes(mayuscula(format(dia, "LLLL yyyy", { locale: es }))))
  }

  return (
    <div className="flex flex-col gap-4">
      <p role="status" className="sr-only">
        {anuncio} {anuncioEstado}
      </p>
      {citasDelMes.error ? <Alert variant="error">{mensajeDeError(citasDelMes.error)}</Alert> : null}

      <div className={TARJETA}>
        <div ref={mesRef} className={cn(modo === "semana" && "max-lg:hidden")}>
          <Calendar
            mode="single"
            required
            selected={dia}
            onSelect={(elegido) => {
              if (elegido) setDia(elegido)
            }}
            onDayClick={tocarDiaDelMes}
            month={mes}
            onMonthChange={setMes}
            today={hoy}
            modifiers={{ marcado: diasConCita }}
            labels={{
              labelDayButton: (fecha, modificadores) =>
                nombreDelDia(fecha, modificadores, NOMBRE_DIA_CON_CITA),
            }}
          />
        </div>
        {modo === "semana" ? (
          <div
            ref={tiraRef}
            className="lg:hidden motion-safe:animate-in motion-safe:duration-150 motion-safe:fade-in-0 motion-safe:slide-in-from-top-1"
          >
            <TiraSemana
              dia={dia}
              hoy={hoy}
              diasConCita={diasConCita}
              onElegir={setDia}
              onSemana={moverSemana}
              onVerMes={verElMes}
            />
          </div>
        ) : null}
        <p className="px-(--page-x) pt-3 text-muted-foreground lg:px-0">{LEYENDA_PUNTO}</p>
      </div>

      <section aria-labelledby="calendario-dia" className="flex flex-col gap-3">
        <h2 id="calendario-dia" className="text-h3">
          {tituloDelDia(esHoy, fechaCompleta(diaTexto))}
        </h2>
        {errorEstado ? <Alert variant="error">{mensajeDeError(errorEstado)}</Alert> : null}
        {delDia.isPending ? (
          <p role="status" className="text-muted-foreground">
            {ESTADO_CARGANDO}
          </p>
        ) : null}
        {/* Si ya falló el mes, el aviso de arriba alcanza: no se repite el mismo texto */}
        {delDia.error && !citasDelMes.error ? (
          <Alert variant="error">{mensajeDeError(delDia.error)}</Alert>
        ) : null}
        {delDia.data && delDia.data.length === 0 ? (
          <p className="text-muted-foreground">{SIN_AVISOS_DEL_DIA}</p>
        ) : null}
        {delDia.data && delDia.data.length > 0 ? (
          <LineaDeTiempoDelDia
            items={delDia.data}
            ahora={ahora}
            ocupadoId={ocupadoId}
            onCambiarEstado={esHoy ? cambiar : undefined}
          />
        ) : null}
      </section>
    </div>
  )
}
