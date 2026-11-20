import { SALTAR_AL_CONTENIDO } from "@/lib/mensajes"

/**
 * "Saltar al contenido": primer elemento de Tab en las dos áreas (persona y operador). Solo se ve con el foco
 * y lleva a `#contenido`, el `<main>` del layout (que tiene tabIndex -1 para recibir el foco).
 */
export function SaltarAlContenido() {
  return (
    <a
      href="#contenido"
      className="sr-only z-50 rounded-md bg-card px-4 py-3 font-semibold text-heading focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
    >
      {SALTAR_AL_CONTENIDO}
    </a>
  )
}
