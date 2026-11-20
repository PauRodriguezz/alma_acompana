import { z } from "zod"

import { CATEGORIAS_MICROCAPSULA } from "@/features/admin/etiquetas"
import { hoyArgentina } from "@/lib/fechas"

import { FORMATO_NO_PERMITIDO } from "./mensajes"

// Mismas reglas y textos que el back (MicrocapsulaService, contrato sección 3.3 y 4):
// el front avisa antes de enviar; el back decide.
export const MAX_TITULO = 120
export const MAX_TEXTO = 5000

// Mismo criterio que MicrocapsulaService (FORMATOS_NO_PERMITIDOS): el texto es Markdown limitado a
// título (##), subtítulo (###), negrita, cursiva, listas y destacado (>). Se rechaza HTML, enlaces,
// imágenes, código, líneas horizontales y los otros niveles de título. "<5" o un ">" suelto son texto.
const FORMATOS_NO_PERMITIDOS: RegExp[] = [
  /<[!?]/,
  /<\/?[A-Za-z][A-Za-z0-9-]*(\s[^<>]*)?\/?>/,
  /<[A-Za-z][A-Za-z0-9+.-]*:[^\s<>]*>/,
  /\[[^\]\n]*\]\([^)\n]*\)/,
  /\[[^\]\n]+\]\[[^\]\n]*\]/,
  /^ {0,3}\[[^\]\n]+\]:\s*\S/m,
  /`|^ {0,3}~~~/m,
  /^ {0,3}([-*_])( *\1){2,} *$/m,
  /^ {0,3}=+ *$/m,
  /^ {0,3}(#|#{4,6})([ \t]|$)/m,
]

export function usaSoloFormatoPermitido(texto: string): boolean {
  return !FORMATOS_NO_PERMITIDOS.some((patron) => patron.test(texto))
}

export const esquemaMicrocapsula = z.object({
  titulo: z
    .string()
    .trim()
    .min(1, "Ingresá el título.")
    .max(MAX_TITULO, `El título puede tener hasta ${MAX_TITULO} caracteres.`),
  texto: z
    .string()
    .trim()
    .min(1, "Ingresá el texto.")
    .max(MAX_TEXTO, `El texto puede tener hasta ${MAX_TEXTO} caracteres.`)
    .refine(usaSoloFormatoPermitido, FORMATO_NO_PERMITIDO),
  categoria: z.enum(CATEGORIAS_MICROCAPSULA, { error: "Elegí una categoría." }),
  imagen: z.string().nullable(),
  fechaPublicacion: z
    .string()
    .min(1, "Ingresá la fecha de publicación.")
    .refine((fecha) => fecha <= hoyArgentina(), "La fecha de publicación no puede ser futura."),
})

export type EntradaMicrocapsula = z.input<typeof esquemaMicrocapsula>
export type DatosMicrocapsula = z.output<typeof esquemaMicrocapsula>

/** Detail de CU-11 cuando falta algún obligatorio; si no, el genérico del contrato. */
export const TEXTO_CAMPOS_REQUERIDOS = "Completá todos los campos requeridos de la microcápsula."
export const TEXTO_REVISAR = "Revisá los datos ingresados."
