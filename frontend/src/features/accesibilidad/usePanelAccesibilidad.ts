import { useContext } from "react"

import { PanelAccesibilidadContext } from "./panelContext"

export function usePanelAccesibilidad() {
  const contexto = useContext(PanelAccesibilidadContext)
  if (!contexto) {
    throw new Error("usePanelAccesibilidad tiene que usarse dentro de <PanelAccesibilidadProvider>.")
  }
  return contexto
}
