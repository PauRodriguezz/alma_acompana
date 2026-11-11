import { MensajeAlma } from "@/components/alma/mensaje-alma"

/**
 * Una pregunta o repregunta del guion como mensaje de Alma (Figma 29:399): la introducción y, destacada
 * debajo como título, la pregunta. Una repregunta trae solo su texto. Texto plano, nunca HTML.
 */
export function MensajeGuion({ texto, pregunta }: { texto: string; pregunta?: string }) {
  return (
    <MensajeAlma>
      {texto ? <p className="break-words whitespace-pre-line">{texto}</p> : null}
      {pregunta ? (
        <p className="mt-3 font-heading text-h3 break-words text-heading lg:text-(length:--text-h2-desktop)">
          {pregunta}
        </p>
      ) : null}
    </MensajeAlma>
  )
}
