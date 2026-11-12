import { describe, expect, it } from "vitest"

import { pideSeguirConLaHistoria } from "./pideSeguirConLaHistoria"

describe("pideSeguirConLaHistoria", () => {
  it.each([
    "quiero seguir con mi historia de vida",
    "sigamos con mi historia",
    "podemos continuar con las preguntas",
    "retomemos la historia de vida",
    "¿Seguimos con mi HISTORIA?",
    "Quiero completar las preguntas, por favor",
    "mi Historia de Vida",
    "Continuemos con la historia.",
  ])("sí: %s", (texto) => {
    expect(pideSeguirConLaHistoria(texto)).toBe(true)
  })

  it.each([
    "me contó una historia",
    "la historia de mi barrio",
    "seguí derecho por esa calle",
    "hoy tengo preguntas para el médico",
    "quiero seguir caminando por la plaza del pueblo donde nací y después contarte una historia",
    "",
  ])("no: %s", (texto) => {
    expect(pideSeguirConLaHistoria(texto)).toBe(false)
  })
})
