import { describe, expect, it } from "vitest"

import { RESPUESTAS_AHORA_NO, RESPUESTAS_CONTARTE_ALGO_MAS } from "./mensajes"
import { rotar } from "./rotar"

describe("rotar", () => {
  it("recorre las variantes en orden y vuelve a empezar", () => {
    const vistas = [0, 1, 2, 3, 4, 5, 6].map((i) => rotar(["a", "b", "c"], i))
    expect(vistas).toEqual(["a", "b", "c", "a", "b", "c", "a"])
  })

  it("nunca repite la misma respuesta dos veces seguidas", () => {
    for (const variantes of [RESPUESTAS_CONTARTE_ALGO_MAS, RESPUESTAS_AHORA_NO]) {
      for (let i = 0; i < 10; i++) expect(rotar(variantes, i)).not.toBe(rotar(variantes, i + 1))
    }
  })
})
