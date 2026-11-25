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
 * "Guardada" si lo está (contrato, sección 2). Toda la tarjeta se puede tocar y tiene un solo
 * destino: el enlace del título, estirado sobre la tarjeta con un ::after. El nombre del enlace
 * es el título, así que el lector de pantalla no repite "Leer" en cada tarjeta.
 * - El ::after va en el título y no en el botón: el botón se desplaza al apretar y, al hacerlo,
 *   se convierte en el bloque del ::after, que se achica y el clic fuera del botón se perdía.
 * - "Leer" es solo decoración (no se enfoca ni se anuncia).
 * - El foco por teclado se dibuja alrededor de toda la tarjeta (has-focus-visible).
 * - Si más adelante se suma otro control a la tarjeta (por ejemplo Guardar), va con `relative z-10`
 *   para quedar por encima del enlace estirado, con su propia zona táctil.
 */
export function TarjetaLectura({
  lectura,
  pestania,
}: {
  lectura: LecturaResumen
  /** Pestaña desde la que se abre: el detalle la conserva para volver. */
  pestania: Pestania
}) {
  return (
    <Card className="relative h-full gap-3 transition-shadow hover:shadow-md has-focus-visible:outline-solid has-focus-visible:outline-(length:--foco-ancho) has-focus-visible:outline-(color:--ring) has-focus-visible:outline-offset-(--foco-separacion) max-lg:rounded-lg max-lg:p-4">
      {/* Imagen decorativa (alt vacío): ocupa el ancho de la tarjeta con proporción fija, sin desbordar */}
      {lectura.imagen ? (
        <img
          src={lectura.imagen}
          alt=""
          loading="lazy"
          className="aspect-video w-full rounded-lg object-cover"
        />
      ) : null}
      <EtiquetasLectura categoria={lectura.categoria} guardada={lectura.guardada} />
      <h2 className="text-h3 wrap-break-word">
        <Link
          to={conPestania(rutaLectura(lectura.id), pestania)}
          className="outline-none after:absolute after:inset-0 after:rounded-[inherit]"
        >
          {lectura.titulo}
        </Link>
      </h2>
      <span
        aria-hidden="true"
        className={buttonVariants({ variant: "secondary", className: "self-start" })}
      >
        {BOTON_LEER}
      </span>
    </Card>
  )
}
