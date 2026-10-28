import type { ReactNode } from "react"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { Clock01Icon, TaskDone01Icon, ViewIcon } from "@hugeicons/core-free-icons"
import { cn } from "cn"

import { Button } from "@/components/ui/button"

import { fechaCompleta, horaDeFechaHora, textoPasoHace } from "./fechas"
import {
  BOTON_DESHACER,
  BOTON_TOMADA,
  CHIP_PENDIENTE,
  CHIP_TOMADA,
  CHIP_VISTA,
  ETIQUETA_FRECUENCIA,
  nombreBotonDeshacer,
  nombreBotonTomada,
  tomadaALas,
} from "./mensajes"
import { TarjetaBase } from "./TarjetaBase"
import type { EstadoNotificacion, ItemAgenda } from "./tipos"

function LineaEstado({
  icono,
  className,
  children,
}: {
  icono: IconSvgElement
  className: string
  children: ReactNode
}) {
  return (
    <p className={cn("flex items-center gap-1 font-semibold", className)}>
      <HugeiconsIcon icon={icono} className="size-5 shrink-0" aria-hidden="true" />
      {children}
    </p>
  )
}

/** El estado como texto con ícono: "Pendiente", "Tomada a las 09:12", "Pasó hace 2 h" o "Vista". */
function EstadoAviso({ item, ahora }: { item: ItemAgenda; ahora: Date }) {
  if (item.estado === "TOMADA") {
    return (
      <LineaEstado icono={TaskDone01Icon} className="text-success">
        {item.fechaHoraToma ? tomadaALas(horaDeFechaHora(item.fechaHoraToma)) : CHIP_TOMADA}
      </LineaEstado>
    )
  }
  if (item.estado === "VISTA") {
    return (
      <LineaEstado icono={ViewIcon} className="text-heading">
        {CHIP_VISTA}
      </LineaEstado>
    )
  }
  if (item.atrasada) {
    return (
      <LineaEstado icono={Clock01Icon} className="text-warning">
        {textoPasoHace(item.fechaHoraProgramada, ahora)}
      </LineaEstado>
    )
  }
  return (
    <LineaEstado icono={Clock01Icon} className="text-heading">
      {CHIP_PENDIENTE}
    </LineaEstado>
  )
}

type Props = {
  item: ItemAgenda
  /** "Ahora" en hora de Argentina (useAhora), para "Pasó hace 2 h". */
  ahora: Date
  /** Pone la hora dentro de la tarjeta. Donde la hora ya está arriba (línea de tiempo, "Para hoy"), no. */
  mostrarHora?: boolean
  /** true mientras se guarda un cambio de estado de este aviso. */
  ocupado?: boolean
  /**
   * Sin esto, la tarjeta solo muestra el estado. Con esto, una medicación lleva "Ya la tomé" o
   * "Deshacer": solo se pasa para los avisos de hoy.
   */
  onCambiarEstado?: (estado: EstadoNotificacion) => void
  /** Estilo del botón "Ya la tomé" ("Para hoy" lo destaca). */
  botonTomada?: "primary" | "secondary"
}

/**
 * Un aviso de la agenda (Lista, "Para hoy" y detalle del día del calendario). Medicación: estado y,
 * en hoy, "Ya la tomé" / "Deshacer". Cita: la tarjeta entera es el enlace a su detalle, donde se
 * marca como vista (sin botones adentro).
 */
export function TarjetaAviso({
  item,
  ahora,
  mostrarHora = true,
  ocupado = false,
  onCambiarEstado,
  botonTomada = "secondary",
}: Props) {
  const { recordatorio } = item
  const hora = mostrarHora ? horaDeFechaHora(item.fechaHoraProgramada) : undefined
  const estado = <EstadoAviso item={item} ahora={ahora} />

  if (recordatorio.tipo === "CITA") {
    return (
      <TarjetaBase
        tipo="CITA"
        hora={hora}
        titulo={recordatorio.titulo}
        secundario={`${fechaCompleta(recordatorio.fechaHora)} · ${recordatorio.lugar}`}
        estado={estado}
        enlace={`/recordatorios/${recordatorio.id}`}
      />
    )
  }

  const nombre = recordatorio.nombreMedicamento
  return (
    <TarjetaBase
      tipo="MEDICACION"
      hora={hora}
      titulo={nombre}
      secundario={`${recordatorio.dosis} · ${ETIQUETA_FRECUENCIA[recordatorio.frecuencia].toLowerCase()}`}
      estado={estado}
      acciones={
        onCambiarEstado ? (
          item.estado === "TOMADA" ? (
            <Button
              variant="outline"
              className="w-full sm:w-auto"
              disabled={ocupado}
              aria-label={nombreBotonDeshacer(nombre)}
              onClick={() => onCambiarEstado("PENDIENTE")}
            >
              {BOTON_DESHACER}
            </Button>
          ) : (
            <Button
              variant={botonTomada}
              className="w-full sm:w-auto"
              disabled={ocupado}
              aria-label={nombreBotonTomada(nombre)}
              onClick={() => onCambiarEstado("TOMADA")}
            >
              {BOTON_TOMADA}
            </Button>
          )
        ) : null
      }
    />
  )
}
