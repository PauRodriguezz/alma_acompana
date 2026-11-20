import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { ArrowRight01Icon, Note01Icon, PuzzleIcon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

import { AvatarAlma } from "@/components/alma/avatar-alma"
import { NAVEGACION_ENTRETENIMIENTO, NAVEGACION_NOMBRE_ALMA } from "@/lib/mensajes"

import {
  PREGUNTA_ALMA,
  TARJETA_JUGAR,
  TARJETA_JUGAR_DESCRIPCION,
  TARJETA_LEER,
  TARJETA_LEER_DESCRIPCION,
} from "./mensajes"
import { RUTA_JUEGOS, RUTA_LECTURAS } from "./rutas"

/**
 * Entretenimiento (Figma 29:449 y 44:1010): la pregunta de Alma y dos tarjetas, "Jugar" y "Leer".
 * En celular las tarjetas van apiladas (ícono, texto y flecha en fila); en escritorio, en dos
 * columnas (ícono arriba y la flecha abajo a la derecha).
 */
export function EntretenimientoPage() {
  return (
    <div className="flex flex-col gap-4">
      <h1>{NAVEGACION_ENTRETENIMIENTO}</h1>

      <p className="flex items-center gap-3">
        <AvatarAlma />
        <span>
          {/* El Figma de celular no muestra "Alma:"; para el lector de pantalla se lee igual */}
          <span className="font-bold max-lg:sr-only">{NAVEGACION_NOMBRE_ALMA}: </span>
          {PREGUNTA_ALMA}
        </span>
      </p>

      <ul className="grid gap-4 lg:grid-cols-2 lg:gap-6">
        <li className="flex">
          <TarjetaAcceso
            to={RUTA_JUEGOS}
            titulo={TARJETA_JUGAR}
            descripcion={TARJETA_JUGAR_DESCRIPCION}
            icono={PuzzleIcon}
            fondo="bg-juego-series-soft"
            circulo="bg-juego-series text-foreground"
          />
        </li>
        <li className="flex">
          <TarjetaAcceso
            to={RUTA_LECTURAS}
            titulo={TARJETA_LEER}
            descripcion={TARJETA_LEER_DESCRIPCION}
            icono={Note01Icon}
            fondo="bg-juego-asociaciones-soft"
            circulo="bg-juego-asociaciones text-primary-foreground"
          />
        </li>
      </ul>
    </div>
  )
}

/**
 * Tarjeta que es un enlace entero. En alto contraste: fondo card y borde de 2 px. El círculo del
 * ícono es decorativo y mide un tamaño fijo en px (no en rem): con letra "Muy grande" crecía hasta
 * dejar 103 px al texto y una palabra se salía de la pantalla a 360 px. El texto sí escala
 * (excepción documentada en el contrato de juegos, sección 10.4).
 */
function TarjetaAcceso({
  to,
  titulo,
  descripcion,
  icono,
  fondo,
  circulo,
}: {
  to: string
  titulo: string
  descripcion: string
  icono: IconSvgElement
  /** Clases completas (Tailwind tiene que encontrarlas escritas). */
  fondo: string
  circulo: string
}) {
  return (
    <Link
      to={to}
      className={`flex w-full items-center gap-4 rounded-lg p-4 transition-shadow hover:shadow-sm lg:items-end lg:rounded-xl lg:p-6 contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card ${fondo}`}
    >
      <span className="flex min-w-0 flex-1 items-center gap-4 lg:flex-col lg:items-start lg:gap-2">
        <span
          className={`inline-flex size-[56px] shrink-0 items-center justify-center rounded-full contraste-alto:border-2 contraste-alto:border-foreground ${circulo}`}
        >
          <HugeiconsIcon icon={icono} className="size-[28px]" aria-hidden="true" />
        </span>
        <span className="flex min-w-0 flex-col gap-1 lg:gap-2">
          <span className="font-heading text-h2 font-medium text-heading lg:text-h3">{titulo}</span>
          <span className="text-foreground">{descripcion}</span>
        </span>
      </span>
      <HugeiconsIcon
        icon={ArrowRight01Icon}
        className="size-[calc(28rem/17)] shrink-0 text-foreground"
        aria-hidden="true"
      />
    </Link>
  )
}
