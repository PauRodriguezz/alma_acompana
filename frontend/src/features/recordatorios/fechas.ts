import { addDays, differenceInMinutes, format, parseISO } from "date-fns"
import { es } from "date-fns/locale"

import { mayuscula } from "@/lib/fechas"

import { pasoHace } from "./mensajes"

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

/** "09:00:00" → "09:00" (la hora de una medicación llega sola, sin fecha). */
export function horaDeToma(hora: string): string {
  return hora.slice(0, 5)
}

/** "2026-10-28T09:12:00" → "09:12" */
export function horaDeFechaHora(fechaHora: string): string {
  return format(parseISO(fechaHora), "HH:mm")
}

/** "2026-10-29" o con hora → "Jueves 29 de octubre". */
export function fechaCompleta(fecha: string): string {
  return mayuscula(format(parseISO(fecha), "EEEE d 'de' MMMM", { locale: es }))
}

/** "Pasó hace 35 min", "Pasó hace 2 h" o "Pasó hace 3 días". */
export function textoPasoHace(fechaHoraProgramada: string, ahora: Date): string {
  const minutos = Math.max(1, differenceInMinutes(ahora, parseISO(fechaHoraProgramada)))
  if (minutos < 60) return pasoHace(`${minutos} min`)
  const horas = Math.floor(minutos / 60)
  if (horas < 24) return pasoHace(`${horas} h`)
  const dias = Math.floor(horas / 24)
  return pasoHace(dias === 1 ? "1 día" : `${dias} días`)
}
