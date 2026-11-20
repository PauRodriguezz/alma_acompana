import { useEffect, useRef, type ReactNode } from "react"

import { SALTAR_AL_CONTENIDO } from "@/lib/mensajes"

// Ayudas para recorrer la app con el teclado y el lector de pantalla: "Saltar al contenido" y el título que recibe el foco.

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

/** El h1 de la pantalla, que recibe el foco al aparecer (contrato de microcápsulas, 10.4). */
export function TituloConFoco({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    ref.current?.focus({ preventScroll: true })
  }, [])
  return (
    <h1 ref={ref} tabIndex={-1} className="wrap-break-word outline-none">
      {children}
    </h1>
  )
}
