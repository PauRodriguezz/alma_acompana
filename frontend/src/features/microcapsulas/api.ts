// Depende de /api/admin/microcapsulas (docs/contrato-panel-operador.md, sección 3.3).
import type { QueryClient } from "@tanstack/react-query"

import { CLAVE_RESUMEN } from "@/features/admin/api"
import { api } from "@/lib/api"

import type { MicrocapsulaCuerpo, MicrocapsulaDetalle, MicrocapsulaResumen } from "./tipos"

/** Query keys del módulo: todas empiezan con "microcapsulas". */
export const CLAVES_MICROCAPSULAS = {
  todas: ["microcapsulas"] as const,
  lista: ["microcapsulas", "lista"] as const,
  detalle: (id: number) => ["microcapsulas", "detalle", id] as const,
}

const RUTA = "/admin/microcapsulas"

export function listarMicrocapsulas(): Promise<MicrocapsulaResumen[]> {
  return api<MicrocapsulaResumen[]>(RUTA)
}

export function obtenerMicrocapsula(id: number): Promise<MicrocapsulaDetalle> {
  return api<MicrocapsulaDetalle>(`${RUTA}/${id}`)
}

export function crearMicrocapsula(cuerpo: MicrocapsulaCuerpo): Promise<MicrocapsulaDetalle> {
  return api<MicrocapsulaDetalle>(RUTA, { method: "POST", cuerpo })
}

export function editarMicrocapsula(
  id: number,
  cuerpo: MicrocapsulaCuerpo
): Promise<MicrocapsulaDetalle> {
  return api<MicrocapsulaDetalle>(`${RUTA}/${id}`, { method: "PUT", cuerpo })
}

/** Baja lógica (204). */
export function darDeBajaMicrocapsula(id: number): Promise<void> {
  return api<void>(`${RUTA}/${id}`, { method: "DELETE" })
}

/** Después de crear, editar o dar de baja: refresca la lista, los detalles y el resumen del Panel. */
export function refrescarMicrocapsulas(cliente: QueryClient) {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: CLAVES_MICROCAPSULAS.todas }),
    cliente.invalidateQueries({ queryKey: CLAVE_RESUMEN }),
  ])
}
