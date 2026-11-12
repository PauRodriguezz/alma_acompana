import { APERTURA_SIN_TITULO, APERTURAS_DE_REPREGUNTA } from "./mensajes"

/**
 * Un título vacío o con forma de pregunta (empieza con "¿" o termina en "?") es el del recuerdo de respaldo
 * que se guarda cuando falló la extracción: no se cita (contrato del mapa de vida, 2.11).
 */
function sePuedeCitar(titulo: string | null | undefined): titulo is string {
  const limpio = titulo?.trim() ?? ""
  return limpio !== "" && !limpio.startsWith("¿") && !limpio.endsWith("?")
}

/**
 * La frase fija que va antes de la primera repregunta de un recuerdo (2.11). `indice` es cuántos recuerdos
 * se abrieron antes en la charla: así las tres frases rotan.
 */
export function aperturaDeRepregunta(titulo: string | null | undefined, indice: number): string {
  if (!sePuedeCitar(titulo)) return APERTURA_SIN_TITULO
  return APERTURAS_DE_REPREGUNTA[indice % APERTURAS_DE_REPREGUNTA.length](titulo.trim())
}
