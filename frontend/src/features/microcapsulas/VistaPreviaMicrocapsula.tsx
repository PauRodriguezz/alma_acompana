import { useId } from "react"

import { TextoLectura } from "@/components/TextoLectura"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import type { CategoriaMicrocapsula } from "@/features/admin/etiquetas"

import { EtiquetaCategoriaMicrocapsula } from "./EtiquetaCategoriaMicrocapsula"
import { ADMIN_VISTA_PREVIA } from "@/features/admin/mensajes"
import { PREVIA_LECTURA, PREVIA_LEER, PREVIA_LISTA, PREVIA_TITULO } from "./mensajes"

type VistaPreviaMicrocapsulaProps = {
  titulo: string
  texto: string
  categoria: CategoriaMicrocapsula | undefined
  imagen: string | null
}

/**
 * "Así lo ve la persona" (Figma 52:2197): primero la tarjeta de la lista (categoría, título,
 * imagen si hay y el botón "Leer") y después la lectura completa, con el texto y su formato
 * dibujado con el mismo componente que usa la persona (TextoLectura). Se actualiza mientras se
 * escribe. El botón es solo de muestra (no hace nada), por eso no se anuncia. El Figma no dibuja
 * la imagen ni el texto en la vista previa.
 */
export function VistaPreviaMicrocapsula({
  titulo,
  texto,
  categoria,
  imagen,
}: VistaPreviaMicrocapsulaProps) {
  const id = useId()
  const tituloLimpio = titulo.trim() || PREVIA_TITULO
  const etiqueta = categoria ? <EtiquetaCategoriaMicrocapsula categoria={categoria} /> : null
  const foto = imagen ? (
    <img src={imagen} alt="" className="aspect-video w-full rounded-lg object-cover" />
  ) : null

  return (
    <section aria-labelledby={id} className="flex flex-col gap-3">
      <h2 id={id} className="font-sans text-body font-bold text-foreground">
        {ADMIN_VISTA_PREVIA}
      </h2>

      <h3 className="font-sans text-body font-semibold text-foreground">{PREVIA_LISTA}</h3>
      <Card className="items-start gap-3">
        {foto}
        {etiqueta}
        <p className="max-w-full font-heading text-h3 font-medium wrap-break-word text-heading">
          {tituloLimpio}
        </p>
        <span
          aria-hidden="true"
          className={`${buttonVariants({ variant: "secondary" })} pointer-events-none`}
        >
          {PREVIA_LEER}
        </span>
      </Card>

      <h3 className="font-sans text-body font-semibold text-foreground">{PREVIA_LECTURA}</h3>
      <Card className="items-start gap-4">
        {etiqueta}
        <p className="max-w-full font-heading text-h1 font-medium wrap-break-word text-heading">
          {tituloLimpio}
        </p>
        {foto}
        <TextoLectura texto={texto} className="w-full" />
      </Card>
    </section>
  )
}
