// Depende de GET /api/admin/panel/resumen (docs/contrato-panel-operador.md, sección 3.1).
import { api } from "@/lib/api"

export type ResumenPanel = {
  ejerciciosActivos: number
  microcapsulasVigentes: number
}

/** Query key del resumen. Ejercicios y microcápsulas la invalidan al crear, editar o dar de baja. */
export const CLAVE_RESUMEN = ["panel", "resumen"] as const

export function obtenerResumen(): Promise<ResumenPanel> {
  return api<ResumenPanel>("/admin/panel/resumen")
}
