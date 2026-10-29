import { useId } from "react"

import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import type { CategoriaMicrocapsula } from "@/features/admin/etiquetas"

import { EtiquetaCategoriaMicrocapsula } from "./EtiquetaCategoriaMicrocapsula"

type VistaPreviaMicrocapsulaProps = {
  titulo: string
  categoria: CategoriaMicrocapsula | undefined
  imagen: string | null
}

/**
 * "Así lo ve la persona" (Figma 52:2197): tarjeta con la etiqueta de categoría, el título
 * en h3 y el botón "Leer". Se actualiza mientras se escribe. El botón es solo de muestra
 * (no hace nada), por eso no se anuncia. Si hay imagen, se muestra arriba (el Figma no la
 * dibuja en la vista previa).
 */
export function VistaPreviaMicrocapsula({ titulo, categoria, imagen }: VistaPreviaMicrocapsulaProps) {
  const id = useId()
  const tituloLimpio = titulo.trim()

  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <h2 id={id} className="font-sans text-body font-bold text-foreground">
        Así lo ve la persona
      </h2>
      <Card className="items-start gap-3">
        {imagen ? (
          <img src={imagen} alt="" className="aspect-video w-full rounded-lg object-cover" />
        ) : null}
        {categoria ? <EtiquetaCategoriaMicrocapsula categoria={categoria} /> : null}
        <h3 className="max-w-full wrap-break-word">{tituloLimpio || "Acá aparece el título"}</h3>
        <span
          aria-hidden="true"
          className={`${buttonVariants({ variant: "secondary" })} pointer-events-none`}
        >
          Leer
        </span>
      </Card>
    </section>
  )
}
