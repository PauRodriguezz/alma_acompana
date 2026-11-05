// Tipos según las respuestas del contrato (docs/contrato-microcapsulas.md, sección 6).
// No llegan fechaPublicacion ni fechaBaja: la persona no los usa.
import type { CategoriaMicrocapsula } from "@/features/admin/etiquetas"

export type { CategoriaMicrocapsula }

/** 6.1: fila de GET /api/microcapsulas y de GET /api/microcapsulas/guardadas. imagen: null o "/imagenes/…". */
export type LecturaResumen = {
  id: number
  titulo: string
  categoria: CategoriaMicrocapsula
  imagen: string | null
  /** Si la persona del token la tiene guardada. En "guardadas", siempre true. */
  guardada: boolean
}

/** 6.2: GET /api/microcapsulas/{id}. El texto trae sus párrafos separados por "\n\n". */
export type LecturaDetalle = LecturaResumen & {
  texto: string
}
