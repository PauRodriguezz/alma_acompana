import { useCallback, useMemo, useRef, useState, type ReactNode } from "react"

import { PanelAccesibilidad } from "@/components/alma/panel-accesibilidad"
import { Dialog } from "@/components/ui/dialog"

import { PanelAccesibilidadContext, type PanelAccesibilidadValor } from "./panel-context"

/**
 * Monta UNA sola vez el panel de accesibilidad (CU-13) y deja abrirlo desde cualquier control
 * (el botón del menú y la fila de Perfil). Como el panel no tiene un botón propio, se guarda
 * el control que tenía el foco al abrirlo y el foco vuelve ahí al cerrar.
 */
export function PanelAccesibilidadProvider({ children }: { children: ReactNode }) {
  const [abierto, setAbierto] = useState(false)
  const origen = useRef<HTMLElement | null>(null)

  const abrir = useCallback(() => {
    origen.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
    setAbierto(true)
  }, [])

  const valor = useMemo<PanelAccesibilidadValor>(() => ({ abrir }), [abrir])

  return (
    <PanelAccesibilidadContext.Provider value={valor}>
      {children}
      <Dialog open={abierto} onOpenChange={setAbierto}>
        <PanelAccesibilidad finalFocus={origen} />
      </Dialog>
    </PanelAccesibilidadContext.Provider>
  )
}
