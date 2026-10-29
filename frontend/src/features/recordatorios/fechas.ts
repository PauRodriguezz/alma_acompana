import { differenceInMinutes, format, parseISO } from "date-fns"
import { es } from "date-fns/locale"

import { mayuscula } from "@/lib/fechas"

import { pasoHace } from "./mensajes"

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
