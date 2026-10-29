import { z } from "zod"

import {
  CATEGORIAS_EJERCICIO,
  FORMATOS_EJERCICIO,
  NIVELES_DIFICULTAD,
  type FormatoEjercicio,
} from "@/features/admin/etiquetas"

import type { EjercicioCuerpo } from "./tipos"

// Mismas reglas y textos que el back (EjercicioService, contrato secciones 3.2 y 4):
// el front avisa antes de enviar; el back decide.
const MAX_CONSIGNA = 300
const MAX_EXPLICACION = 500
const MAX_TEXTO_A_MEMORIZAR = 1000
const MAX_TEXTO_OPCION = 150

export const TEXTO_CAMPOS_REQUERIDOS = "Completá todos los campos requeridos del ejercicio."
export const TEXTO_OPCIONES_MULTIPLE = "Cargá de 3 a 4 opciones y marcá una sola como correcta."
export const TEXTO_OPCIONES_ORDENAR =
  "Cargá de 3 a 5 pasos y asignale a cada uno una posición distinta."
export const TEXTO_REVISAR = "Revisá los datos ingresados."

/** Cuántas opciones (o pasos) se pueden cargar en cada formato. */
export const LIMITES_OPCIONES: Record<FormatoEjercicio, { min: number; max: number }> = {
  OPCION_MULTIPLE: { min: 3, max: 4 },
  ORDENAR: { min: 3, max: 5 },
}

// En el formulario cada opción tiene las dos marcas (correcta y posición); al enviar se
// manda solo la del formato elegido (ver aCuerpo). La posición es texto porque viene de
// un campo numérico que puede quedar vacío.
const esquemaOpcion = z.object({
  texto: z
    .string()
    .trim()
    .min(1, "Ingresá el texto de la opción.")
    .max(MAX_TEXTO_OPCION, `El texto de la opción puede tener hasta ${MAX_TEXTO_OPCION} caracteres.`),
  esCorrecta: z.boolean(),
  imagen: z.string().nullable(),
  posicion: z.string(),
})

/** true si las posiciones son 1..n, todas distintas. */
function posicionesValidas(posiciones: string[]) {
  const n = posiciones.length
  const numeros = posiciones.map((p) => Number(p))
  return (
    numeros.every((p) => Number.isInteger(p) && p >= 1 && p <= n) && new Set(numeros).size === n
  )
}

export const esquemaEjercicio = z
  .object({
    categoria: z.enum(CATEGORIAS_EJERCICIO, { error: "Elegí una categoría." }),
    formato: z.enum(FORMATOS_EJERCICIO, { error: "Elegí un formato." }),
    consigna: z
      .string()
      .trim()
      .min(1, "Ingresá la consigna.")
      .max(MAX_CONSIGNA, `La consigna puede tener hasta ${MAX_CONSIGNA} caracteres.`),
    nivelDificultad: z.enum(NIVELES_DIFICULTAD, { error: "Elegí un nivel de dificultad." }),
    explicacion: z
      .string()
      .trim()
      .max(MAX_EXPLICACION, `La explicación puede tener hasta ${MAX_EXPLICACION} caracteres.`),
    textoAMemorizar: z.string().trim(),
    opciones: z.array(esquemaOpcion),
  })
  .superRefine((datos, ctx) => {
    if (datos.categoria === "MEMORIA") {
      if (!datos.textoAMemorizar) {
        ctx.addIssue({
          code: "custom",
          path: ["textoAMemorizar"],
          message: "Ingresá el texto a memorizar.",
        })
      } else if (datos.textoAMemorizar.length > MAX_TEXTO_A_MEMORIZAR) {
        ctx.addIssue({
          code: "custom",
          path: ["textoAMemorizar"],
          message: `El texto a memorizar puede tener hasta ${MAX_TEXTO_A_MEMORIZAR} caracteres.`,
        })
      }
    }

    // Sin formato elegido (ya marcado como faltante) no se pueden revisar las opciones.
    const limites = LIMITES_OPCIONES[datos.formato] as (typeof LIMITES_OPCIONES)[FormatoEjercicio] | undefined
    if (!limites) return
    const { min, max } = limites
    const cantidad = datos.opciones.length
    const cantidadValida = cantidad >= min && cantidad <= max
    if (datos.formato === "OPCION_MULTIPLE") {
      const correctas = datos.opciones.filter((o) => o.esCorrecta).length
      if (!cantidadValida || correctas !== 1) {
        ctx.addIssue({ code: "custom", path: ["opciones", "root"], message: TEXTO_OPCIONES_MULTIPLE })
      }
    } else if (!cantidadValida || !posicionesValidas(datos.opciones.map((o) => o.posicion))) {
      ctx.addIssue({ code: "custom", path: ["opciones", "root"], message: TEXTO_OPCIONES_ORDENAR })
    }
  })

export type EntradaEjercicio = z.input<typeof esquemaEjercicio>
export type DatosEjercicio = z.output<typeof esquemaEjercicio>
export type OpcionFormulario = EntradaEjercicio["opciones"][number]

/** Opción vacía para agregar (la posición propuesta es la que sigue). */
export function opcionVacia(posicion: number): OpcionFormulario {
  return { texto: "", esCorrecta: false, imagen: null, posicion: String(posicion) }
}

/**
 * Cuerpo para el back. Solo viaja la marca del formato elegido: en opción múltiple
 * ordenCorrecto va en null y en ordenar esCorrecta va en false (si no, el back responde 400).
 * El texto a memorizar solo viaja en MEMORIA y la explicación vacía va como null.
 */
export function aCuerpo(datos: DatosEjercicio): EjercicioCuerpo {
  const ordenar = datos.formato === "ORDENAR"
  return {
    categoria: datos.categoria,
    formato: datos.formato,
    consigna: datos.consigna,
    nivelDificultad: datos.nivelDificultad,
    explicacion: datos.explicacion || null,
    textoAMemorizar: datos.categoria === "MEMORIA" ? datos.textoAMemorizar : null,
    opciones: datos.opciones.map((o) => ({
      texto: o.texto,
      esCorrecta: ordenar ? false : o.esCorrecta,
      imagen: o.imagen,
      ordenCorrecto: ordenar ? Number(o.posicion) : null,
    })),
  }
}
