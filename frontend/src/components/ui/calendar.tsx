import * as React from "react"
import { DayPicker, type DayButtonProps } from "react-day-picker"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  ArrowDown01Icon,
  ArrowLeft01Icon,
  ArrowRight01Icon,
  ArrowUp01Icon,
} from "@hugeicons/core-free-icons"
import { format } from "date-fns"
import { es } from "date-fns/locale"
import { cn } from "cn"

import { AspectoDia } from "@/components/ui/dia-calendario"
import { mayuscula, nombreDelDia } from "@/lib/fechas"
import { CALENDARIO_MES_ANTERIOR, CALENDARIO_MES_SIGUIENTE } from "@/lib/mensajes"

// Calendario basado en react-day-picker (9.14), que da la grilla accesible: un solo tab stop,
// flechas / Inicio / Fin / RePág / AvPág para moverse, Enter o Espacio para elegir, y el cambio
// de mes anunciado. Acá se le suman el idioma (es, semana desde el lunes), los nombres
// accesibles en español y el estilo del Figma, sin la hoja de estilos de la librería.
// - Cada día es un botón de al menos 44 px de ancho y 48 px de alto.
// - Los días con el modificador `marcado` llevan un punto debajo del número; quien usa el
//   calendario agrega a su nombre accesible el texto que lo explica (el color solo no alcanza).
// - El día elegido lleva círculo lleno y negrita; "hoy", un anillo. En alto contraste, negro.

const CLASES = {
  root: "w-full",
  months: "relative flex flex-col",
  month: "flex w-full flex-col",
  nav: "absolute inset-x-0 top-0 flex items-center justify-between",
  button_previous:
    "inline-flex size-12 items-center justify-center rounded-md text-heading hover:bg-secondary aria-disabled:opacity-50",
  button_next:
    "inline-flex size-12 items-center justify-center rounded-md text-heading hover:bg-secondary aria-disabled:opacity-50",
  month_caption: "flex h-12 items-center justify-center",
  caption_label: "font-heading text-h3 font-medium text-heading",
  month_grid: "w-full table-fixed border-collapse",
  // tracking-tighter: con letra "Muy grande" a 360 px, "dom" en negrita no entra en 1/7 del ancho
  weekday: "h-10 text-center text-caption font-semibold tracking-tighter text-muted-foreground",
  day: "p-0 text-center",
}

function Flecha({ orientation }: { orientation?: "up" | "down" | "left" | "right" }) {
  const icono =
    orientation === "left"
      ? ArrowLeft01Icon
      : orientation === "up"
        ? ArrowUp01Icon
        : orientation === "down"
          ? ArrowDown01Icon
          : ArrowRight01Icon
  return <HugeiconsIcon icon={icono} className="size-6" aria-hidden="true" />
}

function BotonDia({ day, modifiers, className, children, ...props }: DayButtonProps) {
  const ref = React.useRef<HTMLButtonElement>(null)
  // Con el teclado, la librería marca el día "enfocado": acá hay que llevarle el foco de verdad.
  React.useEffect(() => {
    if (modifiers.focused) ref.current?.focus()
  }, [modifiers.focused])

  return (
    <button
      ref={ref}
      {...props}
      data-dia={format(day.date, "yyyy-MM-dd")}
      className={cn(
        // 44 px reales de ancho mínimo (no en rem: con letra "Muy grande" no entrarían siete columnas)
        "relative flex h-[calc(48rem/17)] w-full min-w-[44px] items-center justify-center",
        className
      )}
    >
      <AspectoDia
        esHoy={modifiers.today}
        seleccionado={modifiers.selected}
        marcado={modifiers.marcado}
      >
        {children}
      </AspectoDia>
    </button>
  )
}

function Calendar({
  className,
  classNames,
  components,
  labels,
  formatters,
  ...props
}: React.ComponentProps<typeof DayPicker>) {
  return (
    <DayPicker
      locale={es}
      weekStartsOn={1}
      showOutsideDays={false}
      className={cn("w-full", className)}
      classNames={{ ...CLASES, ...classNames }}
      labels={{
        labelDayButton: (fecha, modificadores) => nombreDelDia(fecha, modificadores),
        labelNext: () => CALENDARIO_MES_SIGUIENTE,
        labelPrevious: () => CALENDARIO_MES_ANTERIOR,
        labelGrid: (mes) => mayuscula(format(mes, "LLLL 'de' yyyy", { locale: es })),
        labelWeekday: (dia) => format(dia, "EEEE", { locale: es }),
        ...labels,
      }}
      formatters={{
        formatCaption: (mes) => mayuscula(format(mes, "LLLL yyyy", { locale: es })),
        formatWeekdayName: (dia) => format(dia, "EEE", { locale: es }),
        ...formatters,
      }}
      components={{ Chevron: Flecha, DayButton: BotonDia, ...components }}
      {...props}
    />
  )
}

export { Calendar }
