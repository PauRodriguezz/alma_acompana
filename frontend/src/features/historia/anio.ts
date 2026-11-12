import { PREFIJO_ANIO_APROXIMADO } from "./mensajes"
import type { PrecisionAnio } from "./tipos"

/**
 * Cómo se muestra el año de un recuerdo (contrato, 2.6.1). Sin año devuelve null: el recuerdo va
 * a "Sin fecha" y no se muestra año. `CALCULADA_POR_EDAD` sale de una cuenta, así que va como año simple.
 */
export function formatearAnio(precision: PrecisionAnio | null, anio: number | null): string | null {
  if (anio === null) return null
  if (precision === "APROXIMADA" || precision === "ESTIMADA") {
    return `${PREFIJO_ANIO_APROXIMADO} ${anio}`
  }
  return String(anio)
}
