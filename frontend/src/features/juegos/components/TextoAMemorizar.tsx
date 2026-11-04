import type { Ref } from "react"

import { Button } from "@/components/ui/button"

import { BOTON_YA_LO_RECORDE } from "../mensajes"

/**
 * MEMORIA (CU-05 3.1; el Figma no la dibuja): primero solo el texto y "Ya lo recordé". Al tocarlo
 * el texto se oculta y aparecen la consigna y las opciones; no hay vuelta atrás. El texto es el
 * título de la pantalla en esta fase (recibe el foco al llegar).
 */
export function TextoAMemorizar({
  texto,
  tituloRef,
  onRecordado,
}: {
  texto: string
  tituloRef: Ref<HTMLHeadingElement>
  onRecordado: () => void
}) {
  return (
    <>
      <h1
        ref={tituloRef}
        tabIndex={-1}
        className="rounded-md bg-secondary p-4 text-h2 whitespace-pre-line wrap-break-word outline-none contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card"
      >
        {texto}
      </h1>
      <Button className="w-full" onClick={onRecordado}>
        {BOTON_YA_LO_RECORDE}
      </Button>
    </>
  )
}
