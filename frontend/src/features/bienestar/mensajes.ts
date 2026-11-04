// Textos del módulo de ánimo (contrato, sección 9.1). Los generales (sesión terminada, sin
// conexión, etc.) están en lib/mensajes.ts. La pregunta contextual de cada nivel la manda el back.

import type { NivelAnimo } from "./tipos"

export const PREGUNTA_ANIMO = "¿Cómo te sentís hoy?"
export const NOMBRE_GRUPO_ANIMO = "Cómo te sentís"
export const CAMBIAR_ANIMO = "Cambiar cómo me siento"
export const ERROR_REGISTRAR_ANIMO = "No pudimos anotar cómo te sentís. Probá de nuevo."

export const ETIQUETAS_ANIMO: Record<NivelAnimo, string> = {
  MUY_MAL: "Muy mal",
  MAL: "Mal",
  NEUTRO: "Más o menos",
  BIEN: "Bien",
  MUY_BIEN: "Muy bien",
}

/** Aviso para el lector de pantalla cuando se registra el ánimo por primera vez. */
export function anuncioRegistrado(nivel: NivelAnimo): string {
  return `Anotamos cómo te sentís: ${ETIQUETAS_ANIMO[nivel]}.`
}

/** Aviso para el lector de pantalla cuando se corrige con "Cambiar cómo me siento". */
export function anuncioCorregido(nivel: NivelAnimo): string {
  return `Cambiamos cómo te sentís: ${ETIQUETAS_ANIMO[nivel]}.`
}
