/** Verbos de continuar (contrato del mapa de vida, 2.8 b), ya sin tildes. */
const VERBOS = [
  "seguir",
  "seguimos",
  "sigamos",
  "sigo",
  "continuar",
  "continuemos",
  "retomar",
  "retomemos",
  "completar",
  "completemos",
]

/** Un verbo de continuar y, a no más de 30 caracteres, "historia" o "preguntas". */
const VERBO_Y_OBJETO = new RegExp(`\\b(?:${VERBOS.join("|")})\\b.{0,30}\\b(?:historia|preguntas)\\b`)

/** Minúsculas, sin tildes ni signos, con un solo espacio entre palabras. */
function normalizar(texto: string): string {
  return texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim()
}

/**
 * ¿La persona pidió seguir con su historia de vida? (contrato del mapa de vida, 2.8 b). Es verdadero si el texto
 * contiene "historia de vida", o un verbo de continuar seguido de "historia" o "preguntas" a no más de 30 caracteres.
 * La palabra "historia" sola no alcanza ("me contó una historia"). Es una heurística: puede no detectar algunas frases.
 */
export function pideSeguirConLaHistoria(texto: string): boolean {
  const limpio = normalizar(texto)
  return limpio.includes("historia de vida") || VERBO_Y_OBJETO.test(limpio)
}
