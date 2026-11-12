import { describe, expect, it } from "vitest"

import { aperturaDeRepregunta } from "./aperturaDeRepregunta"

describe("aperturaDeRepregunta", () => {
  it("cita el título y rota las tres frases", () => {
    expect(aperturaDeRepregunta("Mi maestra Elena", 0)).toBe(
      "Gracias por contármelo. Quiero saber un poco más de «Mi maestra Elena»."
    )
    expect(aperturaDeRepregunta("Mi maestra Elena", 1)).toBe("Me gustó lo que contaste. Sigamos con «Mi maestra Elena».")
    expect(aperturaDeRepregunta("Mi maestra Elena", 2)).toBe(
      "Gracias por compartirlo. Un detalle más sobre «Mi maestra Elena»."
    )
    expect(aperturaDeRepregunta("Mi maestra Elena", 3)).toBe(aperturaDeRepregunta("Mi maestra Elena", 0))
  })

  it.each([null, undefined, "", "   ", "¿Dónde naciste?", "Dónde naciste?", "¿Cómo era tu casa"])(
    "sin título citable (%s) es solo 'Gracias por contármelo.'",
    (titulo) => {
      expect(aperturaDeRepregunta(titulo, 1)).toBe("Gracias por contármelo.")
    }
  )
})
