import type { RefObject } from "react"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"

import {
  ICONO_FORMATO_CURSIVA,
  ICONO_FORMATO_DESTACADO,
  ICONO_FORMATO_LISTA,
  ICONO_FORMATO_LISTA_NUMERADA,
  ICONO_FORMATO_NEGRITA,
  ICONO_FORMATO_SUBTITULO,
  ICONO_FORMATO_TITULO,
} from "@/components/iconos"
import { cn } from "@/lib/utils"

import {
  FORMATO_CURSIVA,
  FORMATO_DESTACADO,
  FORMATO_ETIQUETA,
  FORMATO_LISTA,
  FORMATO_LISTA_NUMERADA,
  FORMATO_NEGRITA,
  FORMATO_SUBTITULO,
  FORMATO_TEXTO_EJEMPLO,
  FORMATO_TITULO,
} from "./mensajes"

// Barra de formato del texto (contrato de microcápsulas, sección 4): escribe la marca de Markdown
// en el área de texto (##, ###, **, *, -, 1., >). El área es de react-hook-form y no se vuelve a
// montar: se cambia su valor desde acá (alCambiar) y se deja la selección donde estaba.
// Sin tamaños ni colores: solo la gramática que muestra TextoLectura.

type Seleccion = { texto: string; desde: number; hasta: number }

const PREFIJO_DE_BLOQUE = /^(#{2,3} |[-*] |\d+\. |> )/

type Bloque = { prefijo: (n: number) => string; reconoce: RegExp }

const TITULO: Bloque = { prefijo: () => "## ", reconoce: /^## / }
const SUBTITULO: Bloque = { prefijo: () => "### ", reconoce: /^### / }
const LISTA: Bloque = { prefijo: () => "- ", reconoce: /^[-*] / }
const NUMERADA: Bloque = { prefijo: (n) => `${n}. `, reconoce: /^\d+\. / }
const DESTACADO: Bloque = { prefijo: () => "> ", reconoce: /^> / }

/** Envuelve la selección (o un texto de ejemplo, que queda seleccionado) con la marca. */
function envolver(area: HTMLTextAreaElement, marca: string): Seleccion {
  const { value, selectionStart: i, selectionEnd: f } = area
  const elegido = value.slice(i, f) || FORMATO_TEXTO_EJEMPLO
  const texto = value.slice(0, i) + marca + elegido + marca + value.slice(f)
  return { texto, desde: i + marca.length, hasta: i + marca.length + elegido.length }
}

/** Pone (o saca, si todas las líneas ya lo tienen) el prefijo del bloque en cada línea elegida. */
function aplicarBloque(area: HTMLTextAreaElement, bloque: Bloque): Seleccion {
  const { value, selectionStart: i, selectionEnd: f } = area
  const inicio = value.lastIndexOf("\n", i - 1) + 1
  let fin = value.indexOf("\n", f > i && value[f - 1] === "\n" ? f - 1 : f)
  if (fin === -1) fin = value.length

  const lineas = value.slice(inicio, fin).split("\n")
  const conTexto = lineas.filter((linea) => linea.trim() !== "")
  const yaLoTienen = conTexto.length > 0 && conTexto.every((linea) => bloque.reconoce.test(linea))

  let n = 0
  const nuevas = lineas.map((linea) => {
    if (linea.trim() === "") return linea
    const limpia = linea.replace(PREFIJO_DE_BLOQUE, "")
    if (yaLoTienen) return limpia
    n += 1
    return bloque.prefijo(n) + limpia
  })
  const bloqueNuevo = nuevas.join("\n")
  return {
    texto: value.slice(0, inicio) + bloqueNuevo + value.slice(fin),
    desde: inicio,
    hasta: inicio + bloqueNuevo.length,
  }
}

type BarraFormatoProps = {
  areaRef: RefObject<HTMLTextAreaElement | null>
  /** id del área de texto que formatea (aria-controls). */
  controla: string
  /** Recibe el texto completo ya con el formato aplicado. */
  alCambiar: (texto: string) => void
}

/**
 * Barra de solo íconos pegada arriba del área de texto: forma con ella un solo bloque (barra de
 * borde redondeado arriba y campo abajo, como un editor de texto enriquecido).
 * Es una excepción a "un ícono nunca va solo" (contrato del panel, sección 6): cada botón tiene
 * aria-label con el nombre del formato y una etiqueta que aparece al pasar el mouse y al enfocar con
 * el teclado (aria-hidden: el lector de pantalla ya lee el aria-label). Se opera con Tab y Enter o
 * Espacio. Cada botón mide 44 px como mínimo y escala con la letra.
 */
export function BarraFormato({ areaRef, controla, alCambiar }: BarraFormatoProps) {
  function aplicar(cambio: (area: HTMLTextAreaElement) => Seleccion) {
    const area = areaRef.current
    if (!area) return
    const { texto, desde, hasta } = cambio(area)
    alCambiar(texto)
    // El valor ya cambió; se devuelve el foco al área con la selección en el texto tocado.
    requestAnimationFrame(() => {
      area.focus()
      area.setSelectionRange(desde, hasta)
    })
  }

  return (
    <div
      role="toolbar"
      aria-label={FORMATO_ETIQUETA}
      aria-controls={controla}
      aria-orientation="horizontal"
      className="flex flex-wrap items-center gap-y-1 rounded-t-md border-2 border-b-0 border-input bg-secondary p-1 contraste-alto:border-foreground contraste-alto:bg-card"
    >
      {GRUPOS.map((grupo, i) => (
        <div
          key={i}
          className={cn(
            "flex items-center gap-1",
            i > 0 && "ml-1 border-l-2 border-border pl-1 contraste-alto:border-foreground"
          )}
        >
          {grupo.map(({ etiqueta, icono, cambio }) => (
            <button
              key={etiqueta}
              type="button"
              aria-label={etiqueta}
              onClick={() => aplicar(cambio)}
              className="group relative inline-flex min-h-11 min-w-11 items-center justify-center rounded-md border-2 border-transparent text-heading hover:bg-outline-hover contraste-alto:hover:border-foreground"
            >
              <HugeiconsIcon icon={icono} className="size-6" aria-hidden="true" />
              <span
                aria-hidden="true"
                className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 rounded-md bg-foreground px-2 py-1 text-caption font-semibold whitespace-nowrap text-background group-hover:block group-focus-visible:block contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card contraste-alto:text-foreground"
              >
                {etiqueta}
              </span>
            </button>
          ))}
        </div>
      ))}
    </div>
  )
}

type BotonFormato = {
  etiqueta: string
  icono: IconSvgElement
  cambio: (area: HTMLTextAreaElement) => Seleccion
}

// Títulos | estilo | listas | destacado
const GRUPOS: BotonFormato[][] = [
  [
    { etiqueta: FORMATO_TITULO, icono: ICONO_FORMATO_TITULO, cambio: (a) => aplicarBloque(a, TITULO) },
    {
      etiqueta: FORMATO_SUBTITULO,
      icono: ICONO_FORMATO_SUBTITULO,
      cambio: (a) => aplicarBloque(a, SUBTITULO),
    },
  ],
  [
    { etiqueta: FORMATO_NEGRITA, icono: ICONO_FORMATO_NEGRITA, cambio: (a) => envolver(a, "**") },
    { etiqueta: FORMATO_CURSIVA, icono: ICONO_FORMATO_CURSIVA, cambio: (a) => envolver(a, "*") },
  ],
  [
    { etiqueta: FORMATO_LISTA, icono: ICONO_FORMATO_LISTA, cambio: (a) => aplicarBloque(a, LISTA) },
    {
      etiqueta: FORMATO_LISTA_NUMERADA,
      icono: ICONO_FORMATO_LISTA_NUMERADA,
      cambio: (a) => aplicarBloque(a, NUMERADA),
    },
  ],
  [
    {
      etiqueta: FORMATO_DESTACADO,
      icono: ICONO_FORMATO_DESTACADO,
      cambio: (a) => aplicarBloque(a, DESTACADO),
    },
  ],
]
