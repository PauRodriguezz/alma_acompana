import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import type { Etapa } from "../etapas"
import type { RecuerdoResumen } from "../tipos"
import { masRecuerdos, TITULO_IMAGEN_PREFIJO, TITULO_IMAGEN_SIN_NOMBRE } from "../mensajes"

// Medidas de la imagen de 1080 × 1080 (contrato del mapa de vida, Anexo A y `mapa-descarga-cuadrada.svg`).
// Esta parte es pura (no dibuja): el dibujo está en `dibujarMapa.ts`.

export const LADO_DE_LA_IMAGEN = 1080

/** Puntos de las seis paradas, en el orden del camino: fila 1 a la derecha, fila 2 a la izquierda, fila 3 a la derecha. */
const PUNTOS: readonly (readonly [number, number])[] = [
  [200, 400],
  [600, 400],
  [600, 680],
  [200, 680],
  [200, 960],
  [600, 960],
]

const CAMINO_HASTA_LA_QUINTA =
  "M90 430 C130 430 150 400 200 400 C300 340 500 460 600 400 C700 340 800 460 900 400 C1000 340 1000 740 900 680 " +
  "C800 620 700 740 600 680 C500 620 300 740 200 680 C100 620 100 1020 200 960"
const CAMINO_COMPLETO = `${CAMINO_HASTA_LA_QUINTA} C300 900 500 1020 600 960 C680 910 740 970 790 970`

/** Arbolitos de adorno: centro y escala 1,8. */
export const ARBOLITOS: readonly (readonly [number, number])[] = [
  [1000, 270],
  [95, 545],
  [1000, 825],
  [110, 300],
]

/**
 * Medidas de una parada respecto de su punto (x, y). Todo el grupo (mástil, banderín, ilustración y tarjeta) está 14 px
 * más arriba que en la referencia: así una tarjeta de tres líneas, que es más alta, no se acerca más al camino.
 */
export const PARADA = {
  /** El mástil está 40 px a la derecha del punto. */
  mastilDx: 40,
  mastilArriba: -210,
  mastilAbajo: -26,
  banderinAncho: 214,
  banderinArriba: -204,
  banderinAbajo: -164,
  muesca: 18,
  ilustracionDx: 252,
  ilustracionDy: -184,
  tarjetaAncho: 290,
  tarjetaArriba: -154,
  /** Margen del texto dentro de la tarjeta, a cada lado. */
  tarjetaMargen: 16,
} as const

/** Cuántos títulos muestra la tarjeta de la imagen antes de pasar a "y N más" (contrato del mapa de vida, 2.5). */
export const TITULOS_POR_TARJETA = 3

/**
 * El alto de la tarjeta y dónde va la línea de base de cada línea de texto (desde el borde de arriba de la tarjeta),
 * según cuántas líneas lleva. Con tres líneas la tarjeta es más alta (96 en lugar de 84) y las líneas van más juntas.
 */
export function medidasDeTarjeta(lineas: number): { alto: number; lineasBase: number[] } {
  if (lineas >= 3) return { alto: 96, lineasBase: [28, 54, 80] }
  if (lineas === 2) return { alto: 84, lineasBase: [34, 64] }
  return { alto: 84, lineasBase: [50] }
}

/** El ancho que tiene un título de la tarjeta antes de cortarse con "…". */
export const ANCHO_DEL_TEXTO_DE_TARJETA = PARADA.tarjetaAncho - 2 * PARADA.tarjetaMargen

/** Ancho máximo del título "La vida de {nombre}". */
export const ANCHO_MAXIMO_DEL_TITULO = 940

export type LineaDeTarjeta = { texto: string; destacada: boolean }

export type ParadaDelMapa = {
  etapa: Etapa
  /** 1 a 6, dentro del punto. */
  numero: number
  x: number
  y: number
  vacia: boolean
  /** Hasta tres líneas: los títulos (ya cortados) y, con muchos recuerdos, "y N más". */
  lineas: LineaDeTarjeta[]
  /** Alto de la tarjeta y línea de base de cada línea, desde el borde de arriba (`medidasDeTarjeta`). */
  altoTarjeta: number
  lineasBase: number[]
}

export type DisenoDelMapa = {
  paradas: ParadaDelMapa[]
  /** El trazado del camino (hasta la parada 5 si no hay "Otros momentos"). */
  camino: string
}

/**
 * Qué muestra la tarjeta de la imagen (contrato del mapa de vida, 2.5): hasta tres títulos y, con cuatro o más
 * recuerdos, los dos primeros y "y N más" (N = total − 2). La tarjeta de la pantalla usa otra regla (2.1).
 */
export function resumenDeTarjetaDeImagen(recuerdos: RecuerdoResumen[]): { titulos: string[]; masDe: number } {
  if (recuerdos.length <= TITULOS_POR_TARJETA) return { titulos: recuerdos.map((r) => r.titulo), masDe: 0 }
  const mostrados = TITULOS_POR_TARJETA - 1
  return { titulos: recuerdos.slice(0, mostrados).map((r) => r.titulo), masDe: recuerdos.length - mostrados }
}

/**
 * Corta un título a una sola línea con "…" si no entra en `anchoMaximo` (contrato del mapa de vida, 2.1 y 2.5). Se
 * mide el texto con la tipografía real (`medir`), así que depende del ancho de las letras y no de un número fijo de
 * caracteres.
 */
export function recortarTitulo(texto: string, anchoMaximo: number, medir: (texto: string) => number): string {
  const limpio = texto.trim()
  if (medir(limpio) <= anchoMaximo) return limpio
  let letras = Array.from(limpio)
  while (letras.length > 1) {
    letras = letras.slice(0, -1)
    const candidato = `${letras.join("").trimEnd()}…`
    if (medir(candidato) <= anchoMaximo) return candidato
  }
  return "…"
}

/**
 * Arma el diseño de la imagen a partir de los recuerdos agrupados por etapa (los mismos grupos que el mapa de
 * pantalla): una parada por etapa, con "Otros momentos" solo si hay recuerdos sin etapa.
 */
export function armarDiseno(grupos: EtapaConRecuerdos[], medir: (texto: string) => number): DisenoDelMapa {
  const paradas = grupos.slice(0, PUNTOS.length).map(({ etapa, recuerdos }, i): ParadaDelMapa => {
    const [x, y] = PUNTOS[i]
    const { titulos, masDe } = resumenDeTarjetaDeImagen(recuerdos)
    const lineas: LineaDeTarjeta[] = titulos.map((titulo) => ({
      texto: recortarTitulo(titulo, ANCHO_DEL_TEXTO_DE_TARJETA, medir),
      destacada: false,
    }))
    if (masDe > 0) lineas.push({ texto: masRecuerdos(masDe), destacada: true })
    const { alto, lineasBase } = medidasDeTarjeta(lineas.length)
    return { etapa, numero: i + 1, x, y, vacia: recuerdos.length === 0, lineas, altoTarjeta: alto, lineasBase }
  })
  return { paradas, camino: paradas.length > 5 ? CAMINO_COMPLETO : CAMINO_HASTA_LA_QUINTA }
}

/**
 * Las partes del título (contrato del mapa de vida, 2.5): "La vida de" y el nombre en cursiva, o solo "Mi vida"
 * (sin cursiva) si no se conoce el nombre.
 */
export function partesDelTitulo(nombre: string | null | undefined): { prefijo: string; nombre: string | null } {
  const limpio = nombre?.trim() ?? ""
  return limpio
    ? { prefijo: TITULO_IMAGEN_PREFIJO, nombre: limpio }
    : { prefijo: TITULO_IMAGEN_SIN_NOMBRE, nombre: null }
}
