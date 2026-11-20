import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import type { Etapa } from "../etapas"
import { PREFIJO_ANIO_IMAGEN, TITULO_IMAGEN_PREFIJO, TITULO_IMAGEN_SIN_NOMBRE } from "../mensajes"
import type { RecuerdoResumen } from "../tipos"
import { FUENTE_DE_TEXTO, FUENTE_DE_TITULOS, type Fuente } from "./tipografias"

// Diseño de la imagen descargable (contrato del mapa de vida, 2.5 y Anexo A): 1920 × 1080, un camino en zigzag
// de dos tramos (izquierda a derecha, vuelta en U por la derecha, derecha a izquierda), seis banderines que solo
// marcan el inicio de cada etapa y los recuerdos como paradas sueltas (punto + tarjeta) debajo del camino.
// Esta parte es pura (no dibuja): decide qué paradas entran y dónde va cada cosa, midiendo el texto con la
// tipografía real (`medir`). El dibujo está en `dibujarMapa.ts`.

export const ANCHO_DE_LA_IMAGEN = 1920
export const ALTO_DE_LA_IMAGEN = 1080

/** Cuántas paradas (tarjetas) entran en la imagen sin superponerse: dos tramos de cinco. */
export const MAXIMO_DE_PARADAS = 10
/** Un título se parte en hasta tres líneas; si no entra, baja el tamaño de la letra (nunca se corta con "…"). */
export const MAXIMO_DE_LINEAS = 3
/** Ancho máximo del título "La vida de {nombre}". */
export const ANCHO_MAXIMO_DEL_TITULO = 1300

/** Mide un texto en px con la tipografía y el tamaño indicados. */
export type Medir = (texto: string, fuente: Fuente, tamanio: number) => number

const Y_CAMINO = [365, 770] as const
const ANCHO_TARJETA = 320
const MARGEN_DE_TARJETA = 18
const SEPARACION_CAMINO_TARJETA = 37
const TAMANIO_DEL_TITULO_DE_TARJETA = 28
const TAMANIO_MINIMO_DEL_TITULO = 16
export const TAMANIO_DEL_ANIO = 26
const ALTO_TARJETA_VACIA = 78
export const ANCHO_DEL_TEXTO_DE_TARJETA = ANCHO_TARJETA - 2 * MARGEN_DE_TARJETA

/** Las diez paradas, en el orden del camino: cinco en el tramo de arriba (de izquierda a derecha) y cinco en el de abajo (de derecha a izquierda). Es el centro de la tarjeta. */
const PARADAS: readonly { fila: 0 | 1; cx: number }[] = [
  { fila: 0, cx: 270 },
  { fila: 0, cx: 600 },
  { fila: 0, cx: 930 },
  { fila: 0, cx: 1280 },
  { fila: 0, cx: 1640 },
  { fila: 1, cx: 1650 },
  { fila: 1, cx: 1290 },
  { fila: 1, cx: 920 },
  { fila: 1, cx: 560 },
  { fila: 1, cx: 225 },
]
/** Una etapa que empieza en la sexta parada (la primera del tramo de abajo) pone su banderín justo antes de la vuelta en U, con la cinta hacia la izquierda: a la derecha no hay lugar. */
const PRIMERA_PARADA_DE_ABAJO = 5
const POLO_ANTES_DE_LA_VUELTA = 1780
const POLO_DE_LA_PRIMERA_ETAPA = 100

const POSTE_ALTO = 187
const CINTA_ALTO = 46
const CINTA_ANCHO_MINIMO = 200
export const CINTA_TAMANIO = 26
const ICONO_RADIO = 27
export const CIRCULO_RADIO = 24
export const PUNTO_RADIO = 13

export const CAMINO = "M70 365 H1780 C1880 365 1880 770 1780 770 H90"

/** Dónde se prueba poner un arbolito de adorno; se usan los primeros que no tocan nada. */
const ARBOLES_POSIBLES: readonly (readonly [number, number])[] = [
  [700, 265], [870, 295], [1320, 285], [460, 290], [300, 690], [450, 668],
  [1000, 690], [800, 675], [1500, 690], [300, 300], [1750, 290], [1250, 690],
]
const MAXIMO_DE_ARBOLES = 4

export type Rectangulo = {
  tipo: "titulo" | "pie" | "poste" | "cinta" | "icono" | "circulo" | "punto" | "tarjeta" | "arbol"
  /** Las piezas del mismo grupo (un banderín; un punto con su tarjeta) pueden tocarse. */
  grupo: string
  x: number
  y: number
  ancho: number
  alto: number
}

export type TarjetaDeRecuerdo = {
  x: number
  y: number
  ancho: number
  alto: number
  /** "1948" o "hacia 1957"; null si el recuerdo no tiene año. */
  anio: string | null
  /** Línea de base del año (si hay). */
  lineaBaseDelAnio: number
  /** El título completo, partido en líneas (nunca cortado). */
  lineas: string[]
  tamanio: number
  interlineado: number
  /** Línea de base de la primera línea del título. */
  lineaBaseDelTitulo: number
}

export type ParadaDelMapa = {
  etapa: Etapa
  id: string
  /** El punto sobre el camino. */
  punto: { x: number; y: number }
  tarjeta: TarjetaDeRecuerdo
}

export type TarjetaVacia = { etapa: Etapa; x: number; y: number; ancho: number; alto: number }

export type BanderinDelMapa = {
  etapa: Etapa
  /** 1 a 6, dentro del círculo sobre el camino. */
  numero: number
  poloX: number
  poloArriba: number
  /** Si la cinta y el ícono van a la izquierda del poste (solo el banderín de la vuelta en U). */
  haciaLaIzquierda: boolean
  caminoY: number
  cinta: { x: number; y: number; ancho: number; alto: number }
  icono: { x: number; y: number; radio: number }
}

export type DisenoDelMapa = {
  ancho: number
  alto: number
  camino: string
  banderines: BanderinDelMapa[]
  paradas: ParadaDelMapa[]
  vacias: TarjetaVacia[]
  arboles: [number, number][]
  rectangulos: Rectangulo[]
  /** Cuántos recuerdos hay y cuántos se muestran. */
  total: number
  mostrados: number
}

/** Los recuerdos por año (el más viejo primero); los que no tienen año, al final, en el orden que vienen. */
export function ordenCronologico(recuerdos: RecuerdoResumen[]): RecuerdoResumen[] {
  return recuerdos
    .map((recuerdo, i) => ({ recuerdo, i }))
    .sort((a, b) => (a.recuerdo.anio ?? Infinity) - (b.recuerdo.anio ?? Infinity) || a.i - b.i)
    .map((x) => x.recuerdo)
}

/** "1948", o "hacia 1957" si el año es aproximado o estimado (contrato, 2.5). */
export function anioParaLaImagen(recuerdo: RecuerdoResumen): string | null {
  if (recuerdo.anio === null) return null
  if (recuerdo.precision === "APROXIMADA" || recuerdo.precision === "ESTIMADA") {
    return `${PREFIJO_ANIO_IMAGEN} ${recuerdo.anio}`
  }
  return String(recuerdo.anio)
}

/** Parte un texto en líneas de hasta `ancho` px, sin cortar palabras (salvo una más ancha que la línea). */
export function partirEnLineas(texto: string, ancho: number, medir: (texto: string) => number): string[] {
  const lineas: string[] = []
  let actual = ""
  for (let palabra of texto.trim().split(/\s+/).filter(Boolean)) {
    while (medir(palabra) > ancho && palabra.length > 1) {
      let corte = palabra.length - 1
      while (corte > 1 && medir(palabra.slice(0, corte)) > ancho) corte--
      if (actual) {
        lineas.push(actual)
        actual = ""
      }
      lineas.push(palabra.slice(0, corte))
      palabra = palabra.slice(corte)
    }
    const prueba = actual ? `${actual} ${palabra}` : palabra
    if (!actual || medir(prueba) <= ancho) {
      actual = prueba
    } else {
      lineas.push(actual)
      actual = palabra
    }
  }
  if (actual) lineas.push(actual)
  return lineas.length > 0 ? lineas : [""]
}

/** Si dos rectángulos están separados por al menos `margen` px. */
export function separados(a: Rectangulo, b: Rectangulo, margen: number): boolean {
  return (
    a.x + a.ancho + margen <= b.x ||
    b.x + b.ancho + margen <= a.x ||
    a.y + a.alto + margen <= b.y ||
    b.y + b.alto + margen <= a.y
  )
}

const MARGEN_ENTRE_PIEZAS = 6
const MARGEN_CON_EL_BORDE = 30

/** Hay choque si dos piezas de grupos distintos se tocan (o están a menos de 6 px) o algo se sale del borde. */
export function hayChoques(rectangulos: Rectangulo[]): boolean {
  for (const r of rectangulos) {
    if (
      r.x < MARGEN_CON_EL_BORDE ||
      r.y < MARGEN_CON_EL_BORDE ||
      r.x + r.ancho > ANCHO_DE_LA_IMAGEN - MARGEN_CON_EL_BORDE ||
      r.y + r.alto > ALTO_DE_LA_IMAGEN - MARGEN_CON_EL_BORDE
    ) {
      return true
    }
  }
  for (let i = 0; i < rectangulos.length; i++) {
    for (let j = i + 1; j < rectangulos.length; j++) {
      const a = rectangulos[i]
      const b = rectangulos[j]
      if (a.grupo !== b.grupo && !separados(a, b, MARGEN_ENTRE_PIEZAS)) return true
    }
  }
  return false
}

type Elegidos = RecuerdoResumen[][]

/**
 * Las combinaciones posibles de cuántas paradas recibe cada etapa: al menos una por etapa (una etapa sin
 * recuerdos usa una para su tarjeta punteada) y hasta diez en total.
 */
function combinaciones(disponibles: number[]): number[][] {
  const resultado: number[][] = []
  const actual: number[] = []
  const recorrer = (k: number, suma: number) => {
    if (k === disponibles.length) {
      resultado.push([...actual])
      return
    }
    const faltan = disponibles.length - k - 1
    for (let n = 1; n <= disponibles[k]; n++) {
      if (suma + n + faltan > MAXIMO_DE_PARADAS) break
      actual.push(n)
      recorrer(k + 1, suma + n)
      actual.pop()
    }
  }
  recorrer(0, 0)
  return resultado
}

/**
 * Qué recuerdos se muestran (contrato, 2.5): no todos, solo los que entran en diez paradas. Al menos uno por etapa
 * que tenga recuerdos y el resto por orden cronológico (los que no tienen año, al final), sin "y N más". En cada
 * etapa se muestran los primeros por año.
 */
export function elegirRecuerdos(grupos: EtapaConRecuerdos[]): Elegidos[] {
  const porEtapa = grupos.map((g) => ordenCronologico(g.recuerdos))
  const rango = new Map<string, number>()
  ordenCronologico(grupos.flatMap((g) => g.recuerdos)).forEach((r, i) => rango.set(r.id, i))
  const candidatas = combinaciones(porEtapa.map((r) => Math.max(1, r.length)))
    .map((cantidades) => {
      const elegidos = porEtapa.map((recuerdos, k) => recuerdos.slice(0, cantidades[k]))
      const suma = elegidos.reduce((total, r) => total + r.length, 0)
      const posicion = elegidos.flat().reduce((total, r) => total + (rango.get(r.id) ?? 0), 0)
      return { elegidos, suma, posicion }
    })
    .sort((a, b) => b.suma - a.suma || a.posicion - b.posicion)
  return candidatas.map((c) => c.elegidos)
}

function izquierda(i: number) {
  return PARADAS[i].cx - ANCHO_TARJETA / 2
}
function derecha(i: number) {
  return PARADAS[i].cx + ANCHO_TARJETA / 2
}

/** Dónde va el poste de la etapa que empieza en la parada `i`: en el hueco entre las tarjetas `i − 1` e `i`. */
function poloAntesDe(i: number): number {
  if (i === 0) return POLO_DE_LA_PRIMERA_ETAPA
  if (i === PRIMERA_PARADA_DE_ABAJO) return POLO_ANTES_DE_LA_VUELTA
  return PARADAS[i].fila === 0 ? (derecha(i - 1) + izquierda(i)) / 2 : (izquierda(i - 1) + derecha(i)) / 2
}

function armarTarjeta(recuerdo: RecuerdoResumen, parada: number, medir: Medir): TarjetaDeRecuerdo {
  const anio = anioParaLaImagen(recuerdo)
  let tamanio = TAMANIO_DEL_TITULO_DE_TARJETA
  const partir = () =>
    partirEnLineas(recuerdo.titulo, ANCHO_DEL_TEXTO_DE_TARJETA, (t) => medir(t, FUENTE_DE_TEXTO, tamanio))
  let lineas = partir()
  while (lineas.length > MAXIMO_DE_LINEAS && tamanio > TAMANIO_MINIMO_DEL_TITULO) {
    tamanio -= 2
    lineas = partir()
  }
  const x = izquierda(parada)
  const y = Y_CAMINO[PARADAS[parada].fila] + SEPARACION_CAMINO_TARJETA
  const interlineado = Math.round(tamanio * 1.2)
  const arriba = anio ? 52 : 18
  return {
    x,
    y,
    ancho: ANCHO_TARJETA,
    alto: arriba + tamanio + (lineas.length - 1) * interlineado + 16,
    anio,
    lineaBaseDelAnio: y + 38,
    lineas,
    tamanio,
    interlineado,
    lineaBaseDelTitulo: y + arriba + tamanio,
  }
}

function armarConEleccion(grupos: EtapaConRecuerdos[], elegidos: Elegidos, medir: Medir): DisenoDelMapa {
  const banderines: BanderinDelMapa[] = []
  const paradas: ParadaDelMapa[] = []
  const vacias: TarjetaVacia[] = []
  const rectangulos: Rectangulo[] = []
  let siguiente = 0

  grupos.forEach(({ etapa, recuerdos }, k) => {
    const desde = siguiente
    const fila = PARADAS[desde].fila
    const caminoY = Y_CAMINO[fila]
    const poloX = poloAntesDe(desde)
    const poloArriba = caminoY - POSTE_ALTO
    const ancho = Math.max(CINTA_ANCHO_MINIMO, medir(etapa.nombre, FUENTE_DE_TITULOS, CINTA_TAMANIO) + 52)
    const haciaLaIzquierda = desde === PRIMERA_PARADA_DE_ABAJO
    const cinta = { x: haciaLaIzquierda ? poloX - 6 - ancho : poloX + 6, y: poloArriba + 12, ancho, alto: CINTA_ALTO }
    const icono = {
      x: haciaLaIzquierda ? cinta.x - 14 - ICONO_RADIO : cinta.x + ancho + 14 + ICONO_RADIO,
      y: cinta.y + CINTA_ALTO / 2,
      radio: ICONO_RADIO,
    }
    banderines.push({ etapa, numero: k + 1, poloX, poloArriba, haciaLaIzquierda, caminoY, cinta, icono })
    const grupo = `etapa-${etapa.id}`
    rectangulos.push(
      { tipo: "poste", grupo, x: poloX - 3, y: poloArriba - 7, ancho: 6, alto: POSTE_ALTO - CIRCULO_RADIO + 7 },
      { tipo: "cinta", grupo, ...cinta },
      { tipo: "icono", grupo, x: icono.x - ICONO_RADIO, y: icono.y - ICONO_RADIO, ancho: 2 * ICONO_RADIO, alto: 2 * ICONO_RADIO },
      { tipo: "circulo", grupo, x: poloX - CIRCULO_RADIO, y: caminoY - CIRCULO_RADIO, ancho: 2 * CIRCULO_RADIO, alto: 2 * CIRCULO_RADIO }
    )

    if (recuerdos.length === 0) {
      const x = izquierda(desde)
      const y = caminoY + SEPARACION_CAMINO_TARJETA
      vacias.push({ etapa, x, y, ancho: ANCHO_TARJETA, alto: ALTO_TARJETA_VACIA })
      rectangulos.push({ tipo: "tarjeta", grupo: `vacia-${etapa.id}`, x, y, ancho: ANCHO_TARJETA, alto: ALTO_TARJETA_VACIA })
    }
    elegidos[k].forEach((recuerdo, j) => {
      const indice = desde + j
      const tarjeta = armarTarjeta(recuerdo, indice, medir)
      const punto = { x: PARADAS[indice].cx, y: Y_CAMINO[PARADAS[indice].fila] }
      paradas.push({ etapa, id: recuerdo.id, punto, tarjeta })
      const g = `recuerdo-${recuerdo.id}`
      rectangulos.push(
        { tipo: "punto", grupo: g, x: punto.x - PUNTO_RADIO, y: punto.y - PUNTO_RADIO, ancho: 2 * PUNTO_RADIO, alto: 2 * PUNTO_RADIO },
        { tipo: "tarjeta", grupo: g, x: tarjeta.x, y: tarjeta.y, ancho: tarjeta.ancho, alto: tarjeta.alto }
      )
    })
    siguiente += Math.max(1, elegidos[k].length)
  })

  // El título y la firma también ocupan lugar.
  const partesTitulo = 1300
  rectangulos.push(
    { tipo: "titulo", grupo: "titulo", x: (ANCHO_DE_LA_IMAGEN - partesTitulo) / 2, y: 40, ancho: partesTitulo, alto: 120 },
    { tipo: "pie", grupo: "pie", x: 1500, y: 1000, ancho: 350, alto: 44 }
  )

  const arboles: [number, number][] = []
  for (const [x, y] of ARBOLES_POSIBLES) {
    if (arboles.length >= MAXIMO_DE_ARBOLES) break
    const arbol: Rectangulo = { tipo: "arbol", grupo: `arbol-${x}-${y}`, x: x - 26, y: y - 26, ancho: 52, alto: 80 }
    if (rectangulos.every((r) => separados(arbol, r, 14))) {
      arboles.push([x, y])
      rectangulos.push(arbol)
    }
  }

  const total = grupos.reduce((suma, g) => suma + g.recuerdos.length, 0)
  const mostrados = paradas.length
  return {
    ancho: ANCHO_DE_LA_IMAGEN,
    alto: ALTO_DE_LA_IMAGEN,
    camino: CAMINO,
    banderines,
    paradas,
    vacias,
    arboles,
    rectangulos,
    total,
    mostrados,
  }
}

/**
 * Arma el diseño de la imagen a partir de los recuerdos agrupados por etapa (los mismos grupos que el mapa de
 * pantalla). Prueba las elecciones de recuerdos de la mejor a la peor (más paradas primero y, a igual cantidad, los
 * más antiguos) y se queda con la primera cuya geometría no tiene choques.
 */
export function armarDiseno(grupos: EtapaConRecuerdos[], medir: Medir): DisenoDelMapa {
  const lista = grupos.slice(0, 6)
  const elecciones = elegirRecuerdos(lista)
  let primero: DisenoDelMapa | null = null
  for (const elegidos of elecciones) {
    const diseno = armarConEleccion(lista, elegidos, medir)
    primero ??= diseno
    if (!hayChoques(diseno.rectangulos)) return diseno
  }
  return primero ?? armarConEleccion(lista, lista.map(() => []), medir)
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
