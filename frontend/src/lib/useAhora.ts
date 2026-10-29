import { useEffect, useState } from "react"

import { ahoraArgentina } from "./fechas"

/**
 * "Ahora" en hora de Argentina (ver ahoraArgentina), que se actualiza solo cada minuto: así
 * "Pasó hace 2 h" y el cambio de día se ven sin recargar la página.
 */
export function useAhora(intervaloMs = 60_000): Date {
  const [ahora, setAhora] = useState(ahoraArgentina)

  useEffect(() => {
    const id = window.setInterval(() => setAhora(ahoraArgentina()), intervaloMs)
    return () => window.clearInterval(id)
  }, [intervaloMs])

  return ahora
}
