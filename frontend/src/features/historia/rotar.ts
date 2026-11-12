/** La variante que toca en una rotación: la primera vez la 1.ª, después la 2.ª, y así, volviendo a empezar. */
export function rotar<T>(variantes: readonly T[], vecesUsada: number): T {
  return variantes[vecesUsada % variantes.length]
}
