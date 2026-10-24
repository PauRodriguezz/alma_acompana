import { createContext } from "react"

import type { Rol, Usuario } from "./api"

export type AuthContextValor = {
  token: string | null
  usuario: Usuario | null
  rol: Rol | null
  /** true mientras se recupera la sesión con /api/auth/me al abrir la app. */
  cargando: boolean
  login: (
    nombreUsuario: string,
    contrasenia: string,
    mantenerSesion: boolean
  ) => Promise<Usuario>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValor | null>(null)
