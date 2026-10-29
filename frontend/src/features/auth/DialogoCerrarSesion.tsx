import type { ReactElement } from "react"
import { useNavigate } from "react-router-dom"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"

import { useAuth } from "./useAuth"

// Textos del Figma (Perfil, confirmar cerrar sesión) y del CU-14, camino alterno Nº3.
const TITULO = "¿Querés cerrar sesión?"
const TEXTO = "Para volver a entrar vas a necesitar tu usuario y tu contraseña."
const CONFIRMAR = "Sí, cerrar sesión"

/**
 * Confirmación de cierre de sesión (CU-14), la misma para la persona mayor y para el panel de
 * operador (contrato de login, sección 9; contrato del panel, sección 6). Recibe el botón que
 * la abre como `disparador`. Confirmar llama a logout() y manda a /login.
 */
export function DialogoCerrarSesion({ disparador }: { disparador: ReactElement }) {
  const { logout } = useAuth()
  const navigate = useNavigate()

  function cerrarSesion() {
    logout()
    navigate("/login", { replace: true })
  }

  return (
    <DialogoConfirmar
      titulo={TITULO}
      descripcion={TEXTO}
      textoConfirmar={CONFIRMAR}
      alConfirmar={cerrarSesion}
      disparador={disparador}
    />
  )
}
