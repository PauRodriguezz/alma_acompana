// Depende de /api/admin/ejercicios (docs/contrato-panel-operador.md, sección 3.2).
import type { QueryClient } from "@tanstack/react-query"

import { CLAVE_RESUMEN } from "@/features/admin/api"
import { api } from "@/lib/api"

import type {
  CategoriaEjercicio,
  EjercicioCuerpo,
  EjercicioDetalle,
  EjercicioResumen,
} from "./tipos"

/** Query keys del módulo: todas empiezan con "ejercicios". */
export const CLAVES_EJERCICIOS = {
  todas: ["ejercicios"] as const,
  lista: (categoria: CategoriaEjercicio | null) =>
    ["ejercicios", "lista", categoria ?? "TODAS"] as const,
  detalle: (id: number) => ["ejercicios", "detalle", id] as const,
}

const RUTA = "/admin/ejercicios"

/** Ejercicios activos; con categoría, solo los de esa categoría (filtro del back). */
export function listarEjercicios(categoria: CategoriaEjercicio | null): Promise<EjercicioResumen[]> {
  return api<EjercicioResumen[]>(categoria ? `${RUTA}?categoria=${categoria}` : RUTA)
}

export function obtenerEjercicio(id: number): Promise<EjercicioDetalle> {
  return api<EjercicioDetalle>(`${RUTA}/${id}`)
}

export function crearEjercicio(cuerpo: EjercicioCuerpo): Promise<EjercicioDetalle> {
  return api<EjercicioDetalle>(RUTA, { method: "POST", cuerpo })
}

export function editarEjercicio(id: number, cuerpo: EjercicioCuerpo): Promise<EjercicioDetalle> {
  return api<EjercicioDetalle>(`${RUTA}/${id}`, { method: "PUT", cuerpo })
}

/** Baja lógica (204). */
export function eliminarEjercicio(id: number): Promise<void> {
  return api<void>(`${RUTA}/${id}`, { method: "DELETE" })
}

/** Después de crear, editar o eliminar: refresca las listas, los detalles y el resumen del Panel. */
export function refrescarEjercicios(cliente: QueryClient) {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: CLAVES_EJERCICIOS.todas }),
    cliente.invalidateQueries({ queryKey: CLAVE_RESUMEN }),
  ])
}
