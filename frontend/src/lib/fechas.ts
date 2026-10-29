import { addDays, format, parseISO } from "date-fns"
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

/**
 * Zona del servidor (AlmaApplication, ClockConfig y spring.jackson.time-zone). "Hoy" y "ahora"
 * se calculan en esta zona y no en la del dispositivo: así coinciden con las fechas del back.
 * Es la única vez que aparece en el front.
 */
const ZONA_ARGENTINA = "America/Argentina/Buenos_Aires"

const FORMATO_ARGENTINA = new Intl.DateTimeFormat("en-CA", {
  timeZone: ZONA_ARGENTINA,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hourCycle: "h23",
})

/**
 * Un Date cuyos campos (año, mes, día, hora...) son la hora de pared de Argentina. Sirve para
 * comparar con las fechas del back, que llegan sin zona. No es un instante real.
 */
export function ahoraArgentina(): Date {
  const partes = Object.fromEntries(
    FORMATO_ARGENTINA.formatToParts(new Date()).map((parte) => [parte.type, parte.value])
  )
  return new Date(
    Number(partes.year),
    Number(partes.month) - 1,
    Number(partes.day),
    Number(partes.hour),
    Number(partes.minute),
    Number(partes.second)
  )
}

/** "yyyy-MM-dd" de hoy en Argentina. */
export function hoyArgentina(): string {
  return format(ahoraArgentina(), "yyyy-MM-dd")
}

/** Suma días a una fecha "yyyy-MM-dd" (con parseISO: new Date("yyyy-MM-dd") se corre un día en UTC-3). */
export function sumarDias(fecha: string, dias: number): string {
  return format(addDays(parseISO(fecha), dias), "yyyy-MM-dd")
}
