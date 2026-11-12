import { describe, expect, it } from "vitest"

import { agruparPorEtapa } from "../agruparPorEtapa"
import type { RecuerdoResumen } from "../tipos"
import {
  ANCHO_DEL_TEXTO_DE_TARJETA,
  LADO_DE_LA_IMAGEN,
  PARADA,
  armarDiseno,
  medidasDeTarjeta,
  partesDelTitulo,
  recortarTitulo,
  type ParadaDelMapa,
} from "./diseno"

/** Cada letra mide 10: los 258 px de la tarjeta alcanzan para 25 letras. */
const medir = (texto: string) => Array.from(texto).length * 10

function recuerdo(id: string, titulo: string, etapaDeVida: RecuerdoResumen["etapaDeVida"]): RecuerdoResumen {
  return { id, titulo, anio: null, precision: null, etapaDeVida, resumen: "", faltantes: [] }
}

describe("recortarTitulo", () => {
  it("deja los títulos que entran", () => {
    expect(recortarTitulo("Mi maestra Elena", ANCHO_DEL_TEXTO_DE_TARJETA, medir)).toBe("Mi maestra Elena")
  })

  it("corta con … lo que no entra, sin pasarse del ancho", () => {
    const largo = "El día que mi papá me llevó a conocer el mar por primera vez"
    const corto = recortarTitulo(largo, ANCHO_DEL_TEXTO_DE_TARJETA, medir)
    expect(corto.endsWith("…")).toBe(true)
    expect(medir(corto)).toBeLessThanOrEqual(ANCHO_DEL_TEXTO_DE_TARJETA)
    expect(largo.startsWith(corto.slice(0, -1))).toBe(true)
  })

  it("no deja un espacio antes de los puntos suspensivos", () => {
    expect(recortarTitulo("aaaaaaaaaaaaaaaaaaaaaaaa bbbbbbbbbb", ANCHO_DEL_TEXTO_DE_TARJETA, medir)).not.toMatch(/ …$/)
  })

  it("con una sola palabra enorme deja al menos algo", () => {
    expect(recortarTitulo("x".repeat(200), 5, medir)).toBe("…")
  })
})

describe("armarDiseno", () => {
  it("sin recuerdos: cinco paradas vacías, sin Otros momentos y con el camino hasta la quinta", () => {
    const diseno = armarDiseno(agruparPorEtapa([]), medir)
    expect(diseno.paradas.map((p) => p.numero)).toEqual([1, 2, 3, 4, 5])
    expect(diseno.paradas.every((p) => p.vacia && p.lineas.length === 0)).toBe(true)
    expect(diseno.camino.endsWith("200 960")).toBe(true)
  })

  it("con recuerdos sin etapa suma la parada 6 y el camino completo", () => {
    const diseno = armarDiseno(agruparPorEtapa([recuerdo("a", "Un día muy feliz", null)]), medir)
    expect(diseno.paradas).toHaveLength(6)
    expect(diseno.paradas[5].etapa.nombre).toBe("Otros momentos")
    expect(diseno.paradas[5].numero).toBe(6)
    expect(diseno.paradas[5].lineas).toEqual([{ texto: "Un día muy feliz", destacada: false }])
    expect(diseno.camino.endsWith("790 970")).toBe(true)
  })

  it("la tarjeta muestra hasta 3 títulos y, con 4 o más, 2 títulos y 'y N más'", () => {
    const grupos = agruparPorEtapa([
      recuerdo("1", "Uno", "INFANCIA"),
      recuerdo("2", "Dos", "ADOLESCENCIA"),
      recuerdo("3", "Tres", "ADOLESCENCIA"),
      ...["Cuatro", "Cinco", "Seis"].map((t, i) => recuerdo(`j${i}`, t, "JUVENTUD")),
      ...["A", "B", "C", "D"].map((t, i) => recuerdo(`a${i}`, t, "ADULTEZ")),
      ...["V", "W", "X", "Y", "Z", "Q", "R"].map((t, i) => recuerdo(`v${i}`, t, "VEJEZ")),
    ])
    const [infancia, adolescencia, juventud, adultez, vejez] = armarDiseno(grupos, medir).paradas
    expect(infancia.lineas.map((l) => l.texto)).toEqual(["Uno"])
    expect(adolescencia.lineas.map((l) => l.texto)).toEqual(["Dos", "Tres"])
    expect(juventud.lineas.map((l) => l.texto)).toEqual(["Cuatro", "Cinco", "Seis"])
    expect(adultez.lineas).toEqual([
      { texto: "A", destacada: false },
      { texto: "B", destacada: false },
      { texto: "y 2 más", destacada: true },
    ])
    expect(vejez.lineas.at(-1)).toEqual({ texto: "y 5 más", destacada: true })
    expect(vejez.lineas).toHaveLength(3)
  })

  it("la tarjeta es más alta solo con tres líneas", () => {
    expect([0, 1, 2, 3].map((n) => medidasDeTarjeta(n).alto)).toEqual([84, 84, 84, 96])
  })

  it("corta los títulos largos y sigue el orden de las filas del camino", () => {
    const largo = "El día que mi papá me llevó a conocer el mar por primera vez"
    const diseno = armarDiseno(agruparPorEtapa([recuerdo("1", largo, "INFANCIA")]), medir)
    expect(diseno.paradas[0].lineas[0].texto.endsWith("…")).toBe(true)
    expect(diseno.paradas.map((p) => [p.x, p.y])).toEqual([
      [200, 400],
      [600, 400],
      [600, 680],
      [200, 680],
      [200, 960],
    ])
  })
})

describe("partesDelTitulo", () => {
  it("con nombre: 'La vida de' y el nombre", () => {
    expect(partesDelTitulo("  Ana ")).toEqual({ prefijo: "La vida de", nombre: "Ana" })
  })

  it.each([null, undefined, "", "   "])("sin nombre (%s): 'Mi vida' y sin cursiva", (nombre) => {
    expect(partesDelTitulo(nombre)).toEqual({ prefijo: "Mi vida", nombre: null })
  })
})

// ── Que nada se pise (contrato del mapa de vida, 2.5) ──

/** Puntos del camino, muestreados de su trazado "M x y C x y x y x y …". */
function muestrasDelCamino(camino: string): [number, number][] {
  const numeros = camino.match(/-?\d+(\.\d+)?/g)!.map(Number)
  const puntos: [number, number][] = []
  let [x0, y0] = [numeros[0], numeros[1]]
  for (let i = 2; i + 5 < numeros.length + 0; i += 6) {
    const [x1, y1, x2, y2, x3, y3] = numeros.slice(i, i + 6)
    for (let k = 0; k <= 200; k++) {
      const t = k / 200
      const u = 1 - t
      puntos.push([
        u ** 3 * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t ** 3 * x3,
        u ** 3 * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t ** 3 * y3,
      ])
    }
    ;[x0, y0] = [x3, y3]
  }
  return puntos
}

type Rectangulo = { x0: number; y0: number; x1: number; y1: number }

const distanciaARectangulo = ([px, py]: [number, number], r: Rectangulo) =>
  Math.hypot(Math.max(r.x0 - px, 0, px - r.x1), Math.max(r.y0 - py, 0, py - r.y1))

/** Lo que ocupa una parada: tarjeta, banderín, ilustración y mástil (sin el punto, que va sobre el camino). */
function elementos(p: ParadaDelMapa): Record<string, Rectangulo> {
  const mastilX = p.x + PARADA.mastilDx
  return {
    tarjeta: { x0: mastilX, y0: p.y + PARADA.tarjetaArriba, x1: mastilX + PARADA.tarjetaAncho, y1: p.y + PARADA.tarjetaArriba + p.altoTarjeta },
    banderin: { x0: mastilX, y0: p.y + PARADA.banderinArriba, x1: mastilX + PARADA.banderinAncho, y1: p.y + PARADA.banderinAbajo },
    ilustracion: {
      x0: mastilX + PARADA.ilustracionDx - 30,
      y0: p.y + PARADA.ilustracionDy - 30,
      x1: mastilX + PARADA.ilustracionDx + 30,
      y1: p.y + PARADA.ilustracionDy + 30,
    },
  }
}

const MITAD_DEL_TRAZO = 22
/** Aire mínimo entre el borde del camino y una tarjeta o un banderín. */
const AIRE_MINIMO = 6

/** `cantidad` recuerdos en cada etapa (también en "Otros momentos", si cantidad > 0). */
function conRecuerdosEnCadaEtapa(cantidad: number) {
  const etapas = ["INFANCIA", "ADOLESCENCIA", "JUVENTUD", "ADULTEZ", "VEJEZ", null] as const
  const recuerdos = etapas.flatMap((etapa) =>
    Array.from({ length: cantidad }, (_, i) =>
      recuerdo(`${etapa}-${i}`, `Un título bastante largo para el recuerdo número ${i + 1} de la etapa`, etapa)
    )
  )
  return armarDiseno(agruparPorEtapa(recuerdos), medir)
}

describe.each([0, 1, 2, 3, 4, 6])("con %i recuerdos por etapa nada se pisa", (cantidad) => {
  const diseno = conRecuerdosEnCadaEtapa(cantidad)
  const camino = muestrasDelCamino(diseno.camino)

  it("las tarjetas, los banderines y las ilustraciones quedan separados del camino", () => {
    for (const parada of diseno.paradas) {
      for (const [nombre, rect] of Object.entries(elementos(parada))) {
        const distancia = Math.min(...camino.map((punto) => distanciaARectangulo(punto, rect))) - MITAD_DEL_TRAZO
        expect(distancia, `${parada.etapa.nombre}: ${nombre}`).toBeGreaterThanOrEqual(AIRE_MINIMO)
      }
    }
  })

  it("la tarjeta de cada fila no toca lo de la fila de abajo ni el borde de la imagen", () => {
    const abajoDe: Record<number, number> = { 0: 3, 1: 2, 3: 4, 2: 5 } // mismo lado, fila siguiente
    for (const [i, j] of Object.entries(abajoDe)) {
      const arriba = diseno.paradas[Number(i)]
      const abajo = diseno.paradas[j]
      if (!abajo) continue
      expect(elementos(arriba).tarjeta.y1 + 20).toBeLessThanOrEqual(abajo.y + PARADA.mastilArriba)
    }
    for (const parada of diseno.paradas) {
      const { tarjeta } = elementos(parada)
      expect(tarjeta.x1).toBeLessThanOrEqual(LADO_DE_LA_IMAGEN - 22 - 20)
      expect(tarjeta.y1).toBeLessThanOrEqual(LADO_DE_LA_IMAGEN - 22 - 60)
    }
  })

  it("la primera fila queda debajo del título", () => {
    expect(diseno.paradas[0].y + PARADA.mastilArriba).toBeGreaterThanOrEqual(180)
  })
})
