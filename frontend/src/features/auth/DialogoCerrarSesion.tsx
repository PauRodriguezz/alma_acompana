import type { ReactElement } from "react"

import { DialogoConfirmacion } from "@/components/alma/dialogo-confirmacion"

import { useAuth } from "./useAuth"

// Textos del Figma (Perfil, confirmar cerrar sesión) y del CU-14, camino alterno Nº3.
const TITULO = "¿Querés cerrar sesión?"
const TEXTO = "Para volver a entrar vas a necesitar tu usuario y tu contraseña."
const CONFIRMAR = "Sí, cerrar sesión"
const CANCELAR = "Cancelar"

/**
 * Confirmación de cierre de sesión (CU-14). Recibe como hijo el botón que la abre: cualquier
 * pantalla la puede usar con su propio botón. Confirmar llama a logout(): sin usuario,
 * ProtectedRoute manda a /login.
 */
export function DialogoCerrarSesion({ children }: { children: ReactElement }) {
  const { logout } = useAuth()

  return (
    <DialogoConfirmacion
      titulo={TITULO}
      texto={TEXTO}
      confirmar={CONFIRMAR}
      cancelar={CANCELAR}
      onConfirmar={logout}
    >
      {children}
    </DialogoConfirmacion>
  )
}
