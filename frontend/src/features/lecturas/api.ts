// Depende de /api/microcapsulas del contrato (docs/contrato-microcapsulas.md, sección 6).
// El panel de operador usa /api/admin/microcapsulas (features/microcapsulas): no se mezclan.
import { api } from "@/lib/api"

import type { LecturaDetalle, LecturaResumen } from "./tipos"

const BASE = "/microcapsulas"

/** 6.1 y 5.1: las vigentes, de la más nueva a la más vieja. */
export function listar(): Promise<LecturaResumen[]> {
  return api<LecturaResumen[]>(BASE)
}

/** 6.1 y 5.2: las guardadas por la persona (vigentes o no), de la última guardada a la primera. */
export function guardadas(): Promise<LecturaResumen[]> {
  return api<LecturaResumen[]>(`${BASE}/guardadas`)
}

/** 6.2 y 5.3: 404 si no existe o si está dada de baja y la persona no la guardó. */
export function detalle(id: number): Promise<LecturaDetalle> {
  return api<LecturaDetalle>(`${BASE}/${id}`)
}

/** 6.3 y 5.4: idempotente (204). 404 si no es accesible para la persona. */
export function guardar(id: number): Promise<void> {
  return api<void>(`${BASE}/${id}/guardada`, { method: "PUT" })
}

/** 6.3 y 5.5: idempotente (204). 404 si no existe. */
export function quitar(id: number): Promise<void> {
  return api<void>(`${BASE}/${id}/guardada`, { method: "DELETE" })
}
