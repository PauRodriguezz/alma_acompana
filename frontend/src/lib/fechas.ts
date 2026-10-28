import { format, parseISO } from "date-fns"
import { es } from "date-fns/locale"

import { CALENDARIO_HOY, CALENDARIO_SELECCIONADO } from "./mensajes"

export function mayuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/**
 * "1950-05-12" o "1950-05-12T10:30:00" → "12/05/1950". Se usa parseISO y no new Date("yyyy-MM-dd"):
 * este último interpreta la fecha en UTC y en Argentina (UTC-3) la corre un día para atrás.
 */
export function fechaCorta(fecha: string): string {
  return format(parseISO(fecha), "dd/MM/yyyy")
}

/**
 * Nombre accesible de un día del calendario: "Hoy, jueves 22 de octubre de 2026, tiene una cita,
 * seleccionado". `textoMarcado` es lo que se agrega si el día lleva la marca `marcado`.
 */
export function nombreDelDia(
  fecha: Date,
  modificadores: Record<string, boolean>,
  textoMarcado?: string
): string {
  return [
    modificadores.today ? CALENDARIO_HOY : null,
    format(fecha, "EEEE d 'de' MMMM 'de' yyyy", { locale: es }),
    modificadores.marcado ? textoMarcado : null,
    modificadores.selected ? CALENDARIO_SELECCIONADO : null,
  ]
    .filter(Boolean)
    .join(", ")
}
