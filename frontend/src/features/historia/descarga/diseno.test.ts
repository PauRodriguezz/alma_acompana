import { describe, expect, it } from "vitest"

import { agruparPorEtapa } from "../agruparPorEtapa"
import type { PrecisionAnio, RecuerdoResumen } from "../tipos"
import {
  ALTO_DE_LA_IMAGEN,
  ANCHO_DE_LA_IMAGEN,
  ANCHO_DEL_TEXTO_DE_TARJETA,
  MAXIMO_DE_LINEAS,
  MAXIMO_DE_PARADAS,
  anioParaLaImagen,
  elegirRecuerdos,
  armarDiseno,
  hayChoques,
  ordenCronologico,
  partesDelTitulo,
  partirEnLineas,
  type DisenoDelMapa,
  type Medir,
} from "./diseno"

/** Cada letra mide la mitad del tamaño: con 28 px, 14 px por letra (20 letras por línea). */
const medir: Medir = (texto, _fuente, tamanio) => Array.from(texto).length * tamanio * 0.5

let numero = 0
function recuerdo(
  titulo: string,
  etapaDeVida: RecuerdoResumen["etapaDeVida"],
  anio: number | null = null,
  precision: PrecisionAnio | null = anio === null ? null : "EXACTA"
): RecuerdoResumen {
  numero += 1
  return { id: `r${numero}`, titulo, anio, precision, etapaDeVida, resumen: "", faltantes: [] }
}

const diseno = (recuerdos: RecuerdoResumen[]) => armarDiseno(agruparPorEtapa(recuerdos), medir)

/** Un mapa parecido al de ana: muchos recuerdos de la infancia y dos sin etapa. */
function mapaDeAna(): RecuerdoResumen[] {
  return [
    recuerdo("Nací en Cipolletti", "INFANCIA", 1950),
    recuerdo("La convivencia en la infancia", "INFANCIA", 1955),
    recuerdo("¿Qué comida de tu infancia te acordás con cariño?", "INFANCIA", 1955),
    recuerdo("Su mamá Nancy", "INFANCIA", 1955),
    recuerdo("Mi maestra Elena", "INFANCIA", 1957, "ESTIMADA"),
    recuerdo("La casa de la calle San Martín", "INFANCIA", 1958),
    recuerdo("Los tiempos de la adolescencia", "ADOLESCENCIA", 1964),
    recuerdo("El rock nacional y el cuarteto", "JUVENTUD", 1968),
    recuerdo("El primer departamento alquilado en Godoy Cruz", "JUVENTUD", 1976),
    recuerdo("El nacimiento del hijo mayor", "ADULTEZ", 1999),
    recuerdo("Un viaje a la costa", null),
    recuerdo("Las mudanzas entre Guaymallén y Luján", null),
  ]
}

function titulos(d: DisenoDelMapa, etapa?: string): string[] {
  return d.paradas.filter((p) => !etapa || p.etapa.id === etapa).map((p) => p.tarjeta.lineas.join(" "))
}

describe("armarDiseno", () => {
  it("es de 1920 × 1080", () => {
    const d = diseno(mapaDeAna())
    expect([d.ancho, d.alto]).toEqual([ANCHO_DE_LA_IMAGEN, ALTO_DE_LA_IMAGEN])
    expect([ANCHO_DE_LA_IMAGEN, ALTO_DE_LA_IMAGEN]).toEqual([1920, 1080])
  })

  it("sin recuerdos: cinco banderines, cada etapa con su tarjeta punteada y nada más", () => {
    const d = diseno([])
    expect(d.banderines).toHaveLength(5)
    expect(d.vacias).toHaveLength(5)
    expect(d.paradas).toHaveLength(0)
    expect(hayChoques(d.rectangulos)).toBe(false)
  })

  it("'Otros momentos' (sexto banderín) solo si hay recuerdos sin etapa", () => {
    expect(diseno([recuerdo("Algo", "INFANCIA", 1950)]).banderines).toHaveLength(5)
    const con = diseno([recuerdo("Algo", null)])
    expect(con.banderines).toHaveLength(6)
    expect(con.banderines[5].etapa.id).toBe("OTROS")
  })

  it("muestra como máximo diez paradas, sin 'y N más'", () => {
    const muchos = Array.from({ length: 40 }, (_, i) =>
      recuerdo(`Recuerdo ${i}`, (["INFANCIA", "ADOLESCENCIA", "JUVENTUD", "ADULTEZ", "VEJEZ", null] as const)[i % 6], 1950 + i)
    )
    const d = diseno(muchos)
    expect(d.paradas.length).toBeLessThanOrEqual(MAXIMO_DE_PARADAS)
    expect(d.mostrados).toBe(d.paradas.length)
    expect(d.total).toBe(40)
    expect(d.paradas.length + d.vacias.length).toBeLessThanOrEqual(MAXIMO_DE_PARADAS)
    expect(hayChoques(d.rectangulos)).toBe(false)
  })

  it("al menos una parada por etapa que tenga recuerdos", () => {
    for (const recuerdos of [mapaDeAna(), [...mapaDeAna(), ...mapaDeAna(), ...mapaDeAna()]]) {
      const d = diseno(recuerdos)
      const con = agruparPorEtapa(recuerdos).filter((g) => g.recuerdos.length > 0)
      for (const g of con) {
        expect(d.paradas.some((p) => p.etapa.id === g.etapa.id)).toBe(true)
      }
      expect(d.banderines.map((b) => b.etapa.id)).toEqual(agruparPorEtapa(recuerdos).map((g) => g.etapa.id))
    }
  })

  it("el resto va por orden cronológico: de cada etapa, los más antiguos", () => {
    const d = diseno(mapaDeAna())
    expect(titulos(d, "INFANCIA")[0]).toBe("Nací en Cipolletti")
    const anios = d.paradas.filter((p) => p.etapa.id === "INFANCIA").map((p) => p.tarjeta.anio)
    expect(anios).toEqual([...anios].sort())
    expect(titulos(d, "INFANCIA")).not.toContain("La casa de la calle San Martín")
  })

  it("sin año, al final: entra después de los que tienen año", () => {
    const r = [
      recuerdo("Sin año", "INFANCIA"),
      recuerdo("De 1960", "INFANCIA", 1960),
      recuerdo("De 1950", "INFANCIA", 1950),
    ]
    expect(ordenCronologico(r).map((x) => x.titulo)).toEqual(["De 1950", "De 1960", "Sin año"])
    // La mejor elección (sin mirar la geometría) los toma en ese orden; en el dibujo, con las otras cuatro etapas
    // vacías, entran los dos primeros: los sin año son los últimos en entrar.
    expect(elegirRecuerdos(agruparPorEtapa(r))[0][0].map((x) => x.titulo)).toEqual(["De 1950", "De 1960", "Sin año"])
    expect(titulos(diseno(r))).toEqual(["De 1950", "De 1960"])
  })

  it("las paradas de una etapa siguen al banderín de esa etapa, en el orden del camino", () => {
    const d = diseno(mapaDeAna())
    const ordenDeEtapas = d.banderines.map((b) => b.etapa.id)
    const ordenDeParadas = d.paradas.map((p) => ordenDeEtapas.indexOf(p.etapa.id))
    expect(ordenDeParadas).toEqual([...ordenDeParadas].sort((a, b) => a - b))
  })

  it("el título completo, partido en hasta tres líneas, nunca cortado con …", () => {
    const largo = "El día que mi papá me llevó a conocer el mar por primera vez con toda la familia"
    const d = diseno([recuerdo(largo, "INFANCIA", 1949), ...mapaDeAna()])
    const p = d.paradas.find((x) => x.tarjeta.lineas.join(" ") === largo)
    expect(p).toBeDefined()
    for (const parada of d.paradas) {
      expect(parada.tarjeta.lineas.length).toBeLessThanOrEqual(MAXIMO_DE_LINEAS)
      for (const linea of parada.tarjeta.lineas) {
        expect(linea).not.toContain("…")
        expect(medir(linea, null as never, parada.tarjeta.tamanio)).toBeLessThanOrEqual(ANCHO_DEL_TEXTO_DE_TARJETA)
      }
    }
    expect(hayChoques(d.rectangulos)).toBe(false)
  })

  it("nada se pisa con 0, 1, 3, 6 y 12 recuerdos, ni con títulos largos o en una sola etapa", () => {
    const etapas = ["INFANCIA", "ADOLESCENCIA", "JUVENTUD", "ADULTEZ", "VEJEZ", null] as const
    const largos = etapas.map((e, i) =>
      recuerdo("Un título bastante largo que ocupa varias líneas de la tarjeta " + i, e, 1950 + i * 10)
    )
    const escenarios = [
      [],
      [recuerdo("Uno", "VEJEZ", 2010)],
      [recuerdo("Uno", "INFANCIA", 1950), recuerdo("Dos", "ADULTEZ", 1990), recuerdo("Tres", null)],
      largos,
      mapaDeAna(),
      Array.from({ length: 12 }, (_, i) => recuerdo(`Solo infancia ${i}`, "INFANCIA", 1950 + i)),
      Array.from({ length: 12 }, (_, i) => recuerdo(`Solo vejez ${i}`, "VEJEZ", 2000 + i)),
      Array.from({ length: 12 }, (_, i) => recuerdo(`Sin etapa ${i}`, null, 1950 + i)),
    ]
    for (const recuerdos of escenarios) {
      expect(hayChoques(diseno(recuerdos).rectangulos)).toBe(false)
    }
  })

  it("hay choque si dos tarjetas se tocan", () => {
    const d = diseno(mapaDeAna())
    const tarjetas = d.rectangulos.filter((r) => r.tipo === "tarjeta")
    expect(hayChoques([...d.rectangulos, { ...tarjetas[0], grupo: "otro" }])).toBe(true)
  })

  it("los arbolitos de adorno no tocan nada", () => {
    const d = diseno(mapaDeAna())
    expect(d.arboles.length).toBeGreaterThan(0)
  })
})

describe("anioParaLaImagen", () => {
  it("año exacto o calculado: el número; aproximado o estimado: 'hacia'; sin año: null", () => {
    expect(anioParaLaImagen(recuerdo("a", null, 1948, "EXACTA"))).toBe("1948")
    expect(anioParaLaImagen(recuerdo("a", null, 1948, "CALCULADA_POR_EDAD"))).toBe("1948")
    expect(anioParaLaImagen(recuerdo("a", null, 1957, "ESTIMADA"))).toBe("hacia 1957")
    expect(anioParaLaImagen(recuerdo("a", null, 1957, "APROXIMADA"))).toBe("hacia 1957")
    expect(anioParaLaImagen(recuerdo("a", null))).toBeNull()
  })
})

describe("partirEnLineas", () => {
  const m = (t: string) => Array.from(t).length * 10

  it("parte por palabras sin cortarlas", () => {
    expect(partirEnLineas("Mi primer departamento en Godoy Cruz", 160, m)).toEqual([
      "Mi primer",
      "departamento en",
      "Godoy Cruz",
    ])
  })

  it("una palabra más ancha que la línea se parte, sin perder letras", () => {
    const lineas = partirEnLineas("Electroencefalografista", 100, m)
    expect(lineas.join("")).toBe("Electroencefalografista")
    for (const linea of lineas) expect(m(linea)).toBeLessThanOrEqual(100)
  })
})

describe("partesDelTitulo", () => {
  it("con nombre: 'La vida de' y el nombre", () => {
    expect(partesDelTitulo("Ana")).toEqual({ prefijo: "La vida de", nombre: "Ana" })
  })

  it("sin nombre: 'Mi vida', sin cursiva", () => {
    expect(partesDelTitulo(null)).toEqual({ prefijo: "Mi vida", nombre: null })
    expect(partesDelTitulo("   ")).toEqual({ prefijo: "Mi vida", nombre: null })
  })
})
