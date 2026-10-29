// Contrato del panel, sección 3.3. Las fechas viajan como "yyyy-MM-dd".
import type { CategoriaMicrocapsula } from "@/features/admin/etiquetas"

export type { CategoriaMicrocapsula }

/** Fila de GET /api/admin/microcapsulas. */
export type MicrocapsulaResumen = {
  id: number
  titulo: string
  categoria: CategoriaMicrocapsula
  fechaPublicacion: string
  imagen: string | null
}

/** Detalle (GET /{id}, y respuesta de POST y PUT). */
export type MicrocapsulaDetalle = {
  id: number
  titulo: string
  texto: string
  categoria: CategoriaMicrocapsula
  imagen: string | null
  fechaPublicacion: string
}

/** Cuerpo de POST y PUT. */
export type MicrocapsulaCuerpo = Omit<MicrocapsulaDetalle, "id">
