import { useCallback, useSyncExternalStore } from "react"

import { useAuth } from "@/features/auth/useAuth"

import {
  enviarACuenta,
  marcarTocadas,
  obtenerPreferencias,
  suscribirse,
  usarPreferencias,
} from "./almacen"
import { PREFERENCIAS_POR_DEFECTO, type PreferenciasAccesibilidad } from "./tipos"

/**
 * Preferencias actuales y cómo cambiarlas (CU-13). Cada cambio se aplica al instante, se
 * guarda en el dispositivo y, si hay sesión de PERSONA_MAYOR, se manda a la cuenta con PUT.
 * Un ADMINISTRADOR nunca llama a /api/accesibilidad (daría 403): solo queda en el dispositivo.
 */
export function useAccesibilidad() {
  const preferencias = useSyncExternalStore(suscribirse, obtenerPreferencias)
  const { rol } = useAuth()

  const cambiar = useCallback(
    (parcial: Partial<PreferenciasAccesibilidad>) => {
      const nuevas = { ...obtenerPreferencias(), ...parcial }
      marcarTocadas(true)
      usarPreferencias(nuevas)
      if (rol === "PERSONA_MAYOR") enviarACuenta(nuevas)
    },
    [rol]
  )

  /** NORMAL y sin alto contraste (CU-13, camino alterno Nº1). */
  const restablecer = useCallback(() => cambiar(PREFERENCIAS_POR_DEFECTO), [cambiar])

  return { preferencias, cambiar, restablecer }
}
