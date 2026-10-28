// Contrato del panel, sección 3.2.
import type {
  CategoriaEjercicio,
  FormatoEjercicio,
  NivelDificultad,
} from "@/features/admin/etiquetas"

export type { CategoriaEjercicio, FormatoEjercicio, NivelDificultad }

/** Fila de GET /api/admin/ejercicios. */
export type EjercicioResumen = {
  id: number
  categoria: CategoriaEjercicio
  consigna: string
  formato: FormatoEjercicio
  nivelDificultad: NivelDificultad
}

/** Opción (o paso, en ORDENAR). imagen: null o una ruta "/imagenes/…". */
export type OpcionDetalle = {
  id: number
  texto: string
  esCorrecta: boolean | null
  imagen: string | null
  /** null en opción múltiple; la posición 1..n en ordenar. */
  ordenCorrecto: number | null
}

/** Detalle (GET /{id}, y respuesta de POST y PUT). */
export type EjercicioDetalle = {
  id: number
  categoria: CategoriaEjercicio
  formato: FormatoEjercicio
  consigna: string
  nivelDificultad: NivelDificultad
  explicacion: string | null
  textoAMemorizar: string | null
  fechaAlta: string
  opciones: OpcionDetalle[]
}

export type OpcionCuerpo = Omit<OpcionDetalle, "id">

/** Cuerpo de POST y PUT. */
export type EjercicioCuerpo = Omit<EjercicioDetalle, "id" | "fechaAlta" | "opciones"> & {
  opciones: OpcionCuerpo[]
}
