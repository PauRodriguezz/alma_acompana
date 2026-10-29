import type { ReactElement } from "react"
import { useNavigate } from "react-router-dom"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"
import { useAuth } from "@/features/auth/useAuth"

/** Cerrar sesión con confirmación (contrato de login, sección 9; contrato del panel, sección 6). */
export function DialogoCerrarSesion({ disparador }: { disparador: ReactElement }) {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function cerrarSesion() {
    logout()
    navigate("/login", { replace: true })
  }

  return (
    <DialogoConfirmar
      titulo="¿Querés cerrar sesión?"
      textoConfirmar="Sí, cerrar sesión"
      alConfirmar={cerrarSesion}
      disparador={disparador}
    />
  )
}
