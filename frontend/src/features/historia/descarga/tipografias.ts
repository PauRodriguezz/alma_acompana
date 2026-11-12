// Tipografías de la imagen descargable (contrato del mapa de vida, 2.5). Poppins y Source Sans 3 son las de la app;
// la del nombre (cursiva) es Lora. Para cambiar la del nombre: se cambia FUENTE_DEL_NOMBRE y el import de la fuente
// en `src/index.css` (hoy `@fontsource/lora/latin-500-italic.css`), y nada más.

export type Fuente = {
  familia: string
  peso: number
  estilo: "normal" | "italic"
  /** Qué se usa si la fuente no llega a cargarse. */
  respaldo: string
}

/** El nombre de la persona en el título ("La vida de Ana"): la tipografía que combina con Poppins. */
export const FUENTE_DEL_NOMBRE: Fuente = { familia: "Lora", peso: 500, estilo: "italic", respaldo: "serif" }
/** "La vida de", los números de las paradas, los banderines y el pie. */
export const FUENTE_DE_TITULOS: Fuente = { familia: "Poppins", peso: 500, estilo: "normal", respaldo: "sans-serif" }
/** Los títulos de las tarjetas. */
export const FUENTE_DE_TEXTO: Fuente = { familia: "Source Sans 3", peso: 400, estilo: "normal", respaldo: "sans-serif" }
/** "y N más": el mismo texto, en seminegrita. */
export const FUENTE_DE_TEXTO_FUERTE: Fuente = { ...FUENTE_DE_TEXTO, peso: 600 }

export const FUENTES_DE_LA_IMAGEN: readonly Fuente[] = [
  FUENTE_DEL_NOMBRE,
  FUENTE_DE_TITULOS,
  FUENTE_DE_TEXTO,
  FUENTE_DE_TEXTO_FUERTE,
]

/** El valor de `ctx.font` / `document.fonts.load` de esa fuente al tamaño indicado (en px). */
export function fuenteCss(fuente: Fuente, tamanio: number): string {
  return `${fuente.estilo} ${fuente.peso} ${tamanio}px "${fuente.familia}", ${fuente.respaldo}`
}
