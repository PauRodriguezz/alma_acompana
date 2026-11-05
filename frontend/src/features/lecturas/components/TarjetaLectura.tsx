import { useId } from "react"
import { Link } from "react-router-dom"

import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { rutaLectura } from "@/features/entretenimiento/rutas"

import { BOTON_LEER } from "../mensajes"
import { conPestania, type Pestania } from "../pestanias"
import type { LecturaResumen } from "../tipos"
import { EtiquetasLectura } from "./EtiquetasLectura"

/**
 * Tarjeta de una lectura (Figma 30:561 y 46:1062): categoría, título y "Leer"; con la marca
 * "Guardada" si lo está (contrato, sección 2). Toda la tarjeta se puede tocar, pero el único
 * elemento interactivo es el enlace "Leer", estirado sobre la tarjeta con un ::after: una sola
 * parada de Tab. El título lo describe (aria-describedby) para distinguir un "Leer" de otro.
 */
export function TarjetaLectura({
  lectura,
  pestania,
}: {
  lectura: LecturaResumen
  /** Pestaña desde la que se abre: el detalle la conserva para volver. */
  pestania: Pestania
}) {
  const idTitulo = useId()
  return (
    <Card className="relative h-full gap-3 max-lg:rounded-lg">
      <EtiquetasLectura categoria={lectura.categoria} guardada={lectura.guardada} />
      <h2 id={idTitulo} className="text-h3 wrap-break-word">
        {lectura.titulo}
      </h2>
      <Link
        to={conPestania(rutaLectura(lectura.id), pestania)}
        aria-describedby={idTitulo}
        className={buttonVariants({
          variant: "secondary",
          className: "self-start after:absolute after:inset-0",
        })}
      >
        {BOTON_LEER}
      </Link>
    </Card>
  )
}
