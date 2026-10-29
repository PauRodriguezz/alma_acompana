import type { ReactElement } from "react"
import { useNavigate } from "react-router-dom"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"
import {
  CERRAR_SESION_AVISO,
  CERRAR_SESION_CONFIRMAR,
  CERRAR_SESION_PREGUNTA,
} from "@/lib/mensajes"

import { useAuth } from "./useAuth"

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
      titulo={CERRAR_SESION_PREGUNTA}
      descripcion={CERRAR_SESION_AVISO}
      textoConfirmar={CERRAR_SESION_CONFIRMAR}
      alConfirmar={cerrarSesion}
      disparador={disparador}
    />
  )
}
