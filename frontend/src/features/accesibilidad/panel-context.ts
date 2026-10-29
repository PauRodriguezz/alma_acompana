import { createContext } from "react"

export type PanelAccesibilidadValor = {
  /** Abre el panel de accesibilidad. Al cerrarlo, el foco vuelve al control que lo abrió. */
  abrir: () => void
}

export const PanelAccesibilidadContext = createContext<PanelAccesibilidadValor | null>(null)
