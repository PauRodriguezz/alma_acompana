import { HugeiconsIcon } from "@hugeicons/react"
import { NavLink } from "react-router-dom"

import { ENTRADAS_NAVEGACION } from "@/components/alma/rutas-persona"
import { NAVEGACION_PRINCIPAL } from "@/lib/mensajes"

// Una sola navegación para celular y escritorio (corte en 1024 px, como el Figma).
// - Celular: barra inferior solo de íconos. El nombre queda para el lector de pantalla: los
//   nombres no entran en 1/5 del ancho de 390 px (ver el aviso en la entrega).
// - Escritorio: menú lateral con ícono y texto.
// La entrada activa lleva aria-current="page" (lo pone NavLink) y, además del color, una barra
// (arriba en celular, a la izquierda en escritorio); en alto contraste, también un borde.
const ENTRADA =
  "group relative flex min-h-[calc(56rem/17)] flex-col items-center justify-center gap-1 rounded-md px-1 py-2 text-foreground transition-colors hover:bg-secondary " +
  "before:absolute before:top-0 before:left-1/2 before:h-1 before:w-6 before:-translate-x-1/2 before:rounded-full before:bg-accent before:opacity-0 " +
  "aria-[current=page]:bg-accent-soft aria-[current=page]:before:opacity-100 " +
  "contraste-alto:border-2 contraste-alto:border-transparent contraste-alto:aria-[current=page]:border-foreground contraste-alto:aria-[current=page]:bg-card " +
  "lg:min-h-[calc(48rem/17)] lg:flex-row lg:justify-start lg:gap-3 lg:px-4 " +
  "lg:before:top-1/2 lg:before:left-0 lg:before:h-6 lg:before:w-1 lg:before:translate-x-0 lg:before:-translate-y-1/2"

export function NavegacionPrincipal() {
  return (
    <nav aria-label={NAVEGACION_PRINCIPAL}>
      <ul className="flex lg:flex-col lg:gap-1">
        {ENTRADAS_NAVEGACION.map((entrada) => (
          <li key={entrada.ruta} className="min-w-0 flex-1">
            <NavLink to={entrada.ruta} end={entrada.exacta} className={ENTRADA}>
              <HugeiconsIcon
                icon={entrada.icono}
                className="size-6 shrink-0 group-aria-[current=page]:text-accent contraste-alto:group-aria-[current=page]:text-foreground"
                aria-hidden="true"
              />
              <span className="sr-only font-semibold lg:not-sr-only">{entrada.nombre}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
