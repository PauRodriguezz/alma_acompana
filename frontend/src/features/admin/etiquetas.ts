import type { IconSvgElement } from "@hugeicons/react"
import { AppleIcon, SleepingIcon, Yoga01Icon } from "@hugeicons/core-free-icons"

// Enums del panel de operador y su texto en pantalla (contrato del panel, sección 2).
// Los arreglos van en el orden del contrato: es el orden de los chips y de los filtros.

export const CATEGORIAS_EJERCICIO = ["REFRAN", "SERIE", "ASOCIACION", "MEMORIA"] as const
export type CategoriaEjercicio = (typeof CATEGORIAS_EJERCICIO)[number]

export const FORMATOS_EJERCICIO = ["OPCION_MULTIPLE", "ORDENAR"] as const
export type FormatoEjercicio = (typeof FORMATOS_EJERCICIO)[number]

export const NIVELES_DIFICULTAD = ["BASICO", "INTERMEDIO", "AVANZADO"] as const
export type NivelDificultad = (typeof NIVELES_DIFICULTAD)[number]

export const CATEGORIAS_MICROCAPSULA = [
  "PREVENCION_CAIDAS",
  "CUIDADO_HOGAR",
  "ACTIVIDAD_FISICA",
  "ALIMENTACION",
  "SUENIO_DESCANSO",
  "BIENESTAR_EMOCIONAL",
  "RECOMENDACIONES",
] as const
export type CategoriaMicrocapsula = (typeof CATEGORIAS_MICROCAPSULA)[number]

// El Figma dice "Lista de palabras"; los documentos dicen MEMORIA ("Memoria").
export const TEXTO_CATEGORIA_EJERCICIO: Record<CategoriaEjercicio, string> = {
  REFRAN: "Refranes",
  SERIE: "Series",
  ASOCIACION: "Asociaciones",
  MEMORIA: "Memoria",
}

/** Texto del formulario (chips de formato). */
export const TEXTO_FORMATO: Record<FormatoEjercicio, string> = {
  OPCION_MULTIPLE: "Elegir una opción",
  ORDENAR: "Ordenar los pasos",
}

/** Texto de la columna "Tipo" de la tabla de ejercicios. */
export const TEXTO_FORMATO_TABLA: Record<FormatoEjercicio, string> = {
  OPCION_MULTIPLE: "Opción múltiple",
  ORDENAR: "Ordenar",
}

export const TEXTO_NIVEL: Record<NivelDificultad, string> = {
  BASICO: "Básico",
  INTERMEDIO: "Intermedio",
  AVANZADO: "Avanzado",
}

export const TEXTO_CATEGORIA_MICROCAPSULA: Record<CategoriaMicrocapsula, string> = {
  PREVENCION_CAIDAS: "Prevención de caídas",
  CUIDADO_HOGAR: "Cuidado del hogar",
  ACTIVIDAD_FISICA: "Actividad física",
  ALIMENTACION: "Alimentación",
  SUENIO_DESCANSO: "Sueño y descanso",
  BIENESTAR_EMOCIONAL: "Bienestar emocional",
  RECOMENDACIONES: "Recomendaciones",
}

// Colores de las etiquetas (Badge). Las clases van escritas enteras para que Tailwind las
// encuentre. En alto contraste el Badge pasa a fondo card con borde de 2 px.

/** Ejercicios (Figma 39:276): fondo game-*-soft y un punto de 10 px del color del juego. */
export const ESTILO_CATEGORIA_EJERCICIO: Record<
  CategoriaEjercicio,
  { fondo: string; punto: string }
> = {
  REFRAN: { fondo: "bg-juego-refranes-soft", punto: "bg-juego-refranes" },
  SERIE: { fondo: "bg-juego-series-soft", punto: "bg-juego-series" },
  ASOCIACION: { fondo: "bg-juego-asociaciones-soft", punto: "bg-juego-asociaciones" },
  MEMORIA: { fondo: "bg-juego-palabras-soft", punto: "bg-juego-palabras" },
}

/**
 * Microcápsulas (Figma 51:1740): fondo pastel y, en tres categorías, un ícono de 16 px
 * (el Figma lo pone "cuando el tipo importa"). Los íconos son los equivalentes de Hugeicons.
 * PROPUESTO (el Figma no las dibuja): "Cuidado del hogar" y "Recomendaciones", con tokens
 * que ya existen (warning-soft y secondary) y sin ícono.
 */
export const ESTILO_CATEGORIA_MICROCAPSULA: Record<
  CategoriaMicrocapsula,
  { fondo: string; icono?: IconSvgElement }
> = {
  PREVENCION_CAIDAS: { fondo: "bg-juego-series-soft", icono: Yoga01Icon },
  CUIDADO_HOGAR: { fondo: "bg-warning-soft" },
  ACTIVIDAD_FISICA: { fondo: "bg-juego-asociaciones-soft" },
  ALIMENTACION: { fondo: "bg-success-soft", icono: AppleIcon },
  SUENIO_DESCANSO: { fondo: "bg-info-soft", icono: SleepingIcon },
  BIENESTAR_EMOCIONAL: { fondo: "bg-accent-soft" },
  RECOMENDACIONES: { fondo: "bg-secondary" },
}
