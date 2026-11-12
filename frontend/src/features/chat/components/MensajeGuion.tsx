import { MensajeAlma } from "@/components/alma/mensaje-alma"

/**
 * Una pregunta o repregunta del guion como mensaje de Alma (Figma 29:399): la introducción y, destacada
 * debajo como título, la pregunta. Una repregunta trae su texto, con una frase de apertura antes en otro párrafo
 * (separados por una línea en blanco, contrato del mapa de vida, 2.11). Texto plano, nunca HTML.
 */
export function MensajeGuion({ texto, pregunta }: { texto: string; pregunta?: string }) {
  return (
    <MensajeAlma>
      {texto
        ? texto.split("\n\n").map((parrafo, i) => (
            <p key={i} className={i > 0 ? "mt-3 break-words whitespace-pre-line" : "break-words whitespace-pre-line"}>
              {parrafo}
            </p>
          ))
        : null}
      {pregunta ? (
        <p className="mt-3 font-heading text-h3 break-words text-heading lg:text-(length:--text-h2-desktop)">
          {pregunta}
        </p>
      ) : null}
    </MensajeAlma>
  )
}
