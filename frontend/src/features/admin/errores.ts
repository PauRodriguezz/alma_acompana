import { ApiError } from "@/lib/api"

const TEXTO_CONEXION = "No pudimos conectarnos. Probá de nuevo en un rato."
const TEXTO_GENERICO = "Algo salió mal. Probá de nuevo en un rato."

/**
 * Texto simple para mostrar un error de la API en el panel: sin conexión, el aviso de
 * conexión; si el back mandó un detail (Problem Details), ese texto tal cual; si no, el
 * genérico del contrato (sección 7).
 */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return TEXTO_CONEXION
    return error.detalle ?? TEXTO_GENERICO
  }
  return TEXTO_GENERICO
}
