import { Link } from "react-router-dom"

import almaSaludo from "@/assets/alma_acompana_saludo.webp"
import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { RUTA_ENTRETENIMIENTO, RUTA_JUGAR } from "@/features/entretenimiento/rutas"
import { NAVEGACION_ENTRETENIMIENTO } from "@/lib/mensajes"

import { BOTON_EMPECEMOS, INICIO_TEXTO, INICIO_TITULO, TITULO_JUEGOS } from "../mensajes"

/**
 * Inicio de Juegos (Figma 30:424 y 44:1080): volver a Entretenimiento, el título y la tarjeta de
 * bienvenida con Alma saludando y "Empecemos". El Figma de escritorio no tiene el título "Juegos";
 * se deja en los dos tamaños.
 */
export function JuegosInicioPage() {
  return (
    <div className="flex flex-col gap-4">
      <EnlaceVolver to={RUTA_ENTRETENIMIENTO} texto={NAVEGACION_ENTRETENIMIENTO} />
      <h1 className="lg:text-h1-desktop">{TITULO_JUEGOS}</h1>

      <Card className="items-center text-center max-lg:rounded-lg">
        <img
          src={almaSaludo}
          alt=""
          className="h-[calc(132rem/17)] w-auto object-contain lg:h-[calc(176rem/17)]"
        />
        <h2 className="lg:text-(length:--text-h2-desktop) lg:leading-(--text-h2-desktop--line-height)">
          {INICIO_TITULO}
        </h2>
        <p className="text-muted-foreground">{INICIO_TEXTO}</p>
        <Link to={RUTA_JUGAR} className={buttonVariants({ className: "w-full" })}>
          {BOTON_EMPECEMOS}
        </Link>
      </Card>
    </div>
  )
}
