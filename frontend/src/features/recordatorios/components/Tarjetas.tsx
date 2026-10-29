import type { ReactNode } from "react"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import {
  ArrowRight01Icon,
  Clock01Icon,
  MedicineBottle01Icon,
  Stethoscope02Icon,
  TaskDone01Icon,
  ViewIcon,
} from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"
import { cn } from "cn"

import { Chip } from "@/components/alma/chip"
import { Button } from "@/components/ui/button"

import { fechaCompleta, horaDeFechaHora, horaDeToma, textoPasoHace } from "../fechas"
import {
  BOTON_DESHACER,
  BOTON_EDITAR,
  BOTON_TOMADA,
  CHIP_CITA,
  CHIP_MEDICACION,
  CHIP_PENDIENTE,
  CHIP_TOMADA,
  CHIP_VISTA,
  ETIQUETA_FRECUENCIA,
  nombreBotonDeshacer,
  nombreBotonTomada,
  tomadaALas,
} from "../mensajes"
import type { EstadoNotificacion, ItemAgenda, Medicacion, TipoRecordatorio } from "../tipos"

// ─────────────────────────────────────────────────────────────────────────────────────────────
// TarjetaBase (interna): la presentación. TarjetaAviso y TarjetaMedicacion le ponen los datos.
// ─────────────────────────────────────────────────────────────────────────────────────────────

// Cada tipo se distingue por ícono, etiqueta de texto y acento lateral (nunca solo color).
const TIPOS = {
  MEDICACION: {
    icono: MedicineBottle01Icon,
    etiqueta: CHIP_MEDICACION,
    tono: "info",
    fondo: "bg-info-soft",
    acento: "border-l-info",
  },
  CITA: {
    icono: Stethoscope02Icon,
    etiqueta: CHIP_CITA,
    tono: "acento",
    fondo: "bg-accent-soft",
    acento: "border-l-accent",
  },
} as const

// Borde visible contra el fondo y acento lateral. En alto contraste todo el borde es negro.
const TARJETA =
  "flex w-full items-stretch gap-3 rounded-lg border-2 border-l-8 border-border bg-card p-4 text-left [box-shadow:var(--sombra-sm)] contraste-alto:border-foreground"

type PropsBase = {
  tipo: TipoRecordatorio
  titulo: string
  /** Dato secundario, en gris. */
  secundario?: string
  /** "09:00": solo donde la hora no está ya arriba (la línea de tiempo la pone aparte). */
  hora?: string
  /** Estado como texto con ícono. */
  estado?: ReactNode
  /** Botones. Una tarjeta con `enlace` no lleva botones adentro. */
  acciones?: ReactNode
  /** Si hay enlace, toda la tarjeta es el enlace y lleva una flecha al costado. */
  enlace?: string
  /** Texto visible de la acción del enlace (por ejemplo "Editar"). */
  textoEnlace?: string
}

/**
 * Tarjeta de un recordatorio o de un aviso (Lista, "Para hoy" y detalle del día del calendario):
 * cuadradito con el ícono del tipo sobre fondo suave, acento lateral, etiqueta del tipo, título,
 * dato secundario y estado. Es solo presentación. El que la usa la envuelve en un <li> si va en
 * una lista.
 */
function TarjetaBase({
  tipo,
  titulo,
  secundario,
  hora,
  estado,
  acciones,
  enlace,
  textoEnlace,
}: PropsBase) {
  const { icono, etiqueta, tono, fondo, acento } = TIPOS[tipo]

  const contenido = (
    <>
      <span
        aria-hidden="true"
        className={cn(
          "flex size-12 shrink-0 items-center justify-center rounded-md text-heading contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card",
          fondo
        )}
      >
        <HugeiconsIcon icon={icono} className="size-6" />
      </span>
      <div className="flex min-w-0 flex-1 flex-col items-start gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Chip tono={tono}>{etiqueta}</Chip>
          {hora ? <span className="font-heading font-medium text-heading">{hora}</span> : null}
        </div>
        <p className="font-bold break-words">{titulo}</p>
        {secundario ? <p className="text-muted-foreground break-words">{secundario}</p> : null}
        {estado}
        {textoEnlace ? <span className="font-semibold text-heading">{textoEnlace}</span> : null}
        {acciones}
      </div>
      {enlace ? (
        <HugeiconsIcon
          icon={ArrowRight01Icon}
          className="size-6 shrink-0 self-center text-heading"
          aria-hidden="true"
        />
      ) : null}
    </>
  )

  if (enlace) {
    return (
      <Link
        to={enlace}
        className={cn(TARJETA, acento, "transition-colors hover:bg-secondary contraste-alto:border-l-foreground")}
      >
        {contenido}
      </Link>
    )
  }
  return <div className={cn(TARJETA, acento, "contraste-alto:border-l-foreground")}>{contenido}</div>
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// TarjetaAviso: un aviso de la agenda
// ─────────────────────────────────────────────────────────────────────────────────────────────

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

type PropsAviso = {
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
}: PropsAviso) {
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

// ─────────────────────────────────────────────────────────────────────────────────────────────
// TarjetaMedicacion: una medicación guardada
// ─────────────────────────────────────────────────────────────────────────────────────────────

/**
 * Una medicación guardada ("Mis medicaciones"): la tarjeta entera lleva a su edición, donde
 * también se elimina. No tiene estado: eso es de cada aviso (TarjetaAviso).
 */
export function TarjetaMedicacion({ medicacion }: { medicacion: Medicacion }) {
  return (
    <TarjetaBase
      tipo="MEDICACION"
      titulo={medicacion.nombreMedicamento}
      secundario={`${medicacion.dosis} · ${ETIQUETA_FRECUENCIA[medicacion.frecuencia].toLowerCase()} · ${horaDeToma(medicacion.horaToma)}`}
      enlace={`/recordatorios/${medicacion.id}/editar`}
      textoEnlace={BOTON_EDITAR}
    />
  )
}
