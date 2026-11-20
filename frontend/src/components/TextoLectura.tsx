import type { ComponentType, ElementType } from "react"
import ReactMarkdown, { type Components, type ExtraProps } from "react-markdown"

import { cn } from "@/lib/utils"

// Texto de una microcápsula (contrato de microcápsulas, sección 4): Markdown limitado a título (##),
// subtítulo (###), negrita, cursiva, listas y destacado (>). Lo usan el detalle de la lectura y la
// vista previa del panel, así lo que ve el operador es lo que ve la persona.
// - Seguro: react-markdown arma elementos de React (no inserta HTML) y acá se ignora el HTML crudo
//   (skipHtml); enlaces, imágenes, código y demás se muestran solo como texto (unwrapDisallowed).
// - Los saltos de línea simples se respetan (whitespace-pre-line) en párrafos y ítems, como antes
//   de que hubiera formato: así los textos viejos se ven igual.
// - Todo escala con la letra de la persona: solo usa los tamaños de index.css (rem), sin tamaños
//   ni colores libres. El destacado lleva borde y fondo; en alto contraste, borde de 2 px.
const ELEMENTOS_PERMITIDOS = ["p", "h2", "h3", "strong", "em", "ul", "ol", "li", "blockquote"]

type Propiedades = ExtraProps & { className?: string }

/** Elemento con sus clases. Quita `node` (lo agrega react-markdown) para que no llegue al DOM. */
function conClases(Etiqueta: ElementType, clases: string): ComponentType<Propiedades> {
  return function Elemento({ node, ...props }) {
    void node
    return <Etiqueta className={clases} {...props} />
  }
}

const COMPONENTES: Components = {
  p: conClases("p", "whitespace-pre-line"),
  h2: conClases("h2", "mt-2"),
  h3: conClases("h3", "mt-1"),
  ul: conClases("ul", "flex list-disc flex-col gap-2 pl-6"),
  ol: conClases("ol", "flex list-decimal flex-col gap-2 pl-6"),
  li: conClases("li", "whitespace-pre-line"),
  blockquote: conClases(
    "blockquote",
    "flex flex-col gap-3 rounded-lg border-l-4 border-primary bg-accent-soft px-4 py-3 font-semibold contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card"
  ),
}

export function TextoLectura({ texto, className }: { texto: string; className?: string }) {
  return (
    <div className={cn("flex flex-col gap-4 text-body wrap-break-word", className)}>
      <ReactMarkdown
        allowedElements={ELEMENTOS_PERMITIDOS}
        unwrapDisallowed
        skipHtml
        components={COMPONENTES}
      >
        {texto}
      </ReactMarkdown>
    </div>
  )
}
