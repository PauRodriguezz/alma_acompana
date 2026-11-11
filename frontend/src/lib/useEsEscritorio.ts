import { useSyncExternalStore } from "react"

// Mismo punto de corte que el resto de la app: `lg` de Tailwind, 64rem (1024 px a letra normal).
const ESCRITORIO = "(min-width: 64rem)"

function suscribirse(alCambiar: () => void) {
  const consulta = window.matchMedia(ESCRITORIO)
  consulta.addEventListener("change", alCambiar)
  return () => consulta.removeEventListener("change", alCambiar)
}

/** true desde `lg` (escritorio); false por debajo (celular). Se actualiza si cambia el ancho. */
export function useEsEscritorio(): boolean {
  return useSyncExternalStore(
    suscribirse,
    () => window.matchMedia(ESCRITORIO).matches,
    () => false
  )
}
