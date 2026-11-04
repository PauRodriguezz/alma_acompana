// Depende de /api/animo del contrato (docs/contrato-animo.md, secciones 4 y 7).

/** Escala de ánimo del back (de menos a más). La API usa el enum, no el número. */
export type NivelAnimo = "MUY_MAL" | "MAL" | "NEUTRO" | "BIEN" | "MUY_BIEN"

/** Lo que devuelven el POST y el PUT (7.1 y 7.2). `fechaHora` llega sin zona: se muestra tal cual. */
export type RegistroAnimo = {
  id: number
  nivelAnimo: NivelAnimo
  fechaHora: string
  preguntaContextual: string
}

/** Niveles en el orden en que se muestran las caritas, de izquierda a derecha (Figma: de "Muy bien" a "Muy mal"). */
export const NIVELES_ANIMO: readonly NivelAnimo[] = ["MUY_BIEN", "BIEN", "NEUTRO", "MAL", "MUY_MAL"]
