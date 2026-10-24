import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"

import { MENSAJE_SESION_VENCIDA, registrarSesionVencida } from "@/lib/api"
import { borrarToken, guardarToken, leerToken } from "@/lib/token"

import { login as loginApi, me, type Usuario } from "./api"
import { AuthContext, type AuthContextValor } from "./auth-context"
import { aplicarPreferencias } from "./preferencias"

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [token, setToken] = useState<string | null>(() => leerToken())
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  // Con token guardado hay que consultar /api/auth/me antes de saber quién es.
  const [cargando, setCargando] = useState(() => leerToken() !== null)

  // Recupera la sesión al abrir la app; si falla, descarta el token.
  useEffect(() => {
    if (!leerToken()) return
    let cancelado = false
    me()
      .then((u) => {
        if (cancelado) return
        setUsuario(u)
        aplicarPreferencias(u.preferenciasAccesibilidad)
      })
      .catch(() => {
        borrarToken()
        if (!cancelado) setToken(null)
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [])

  // Un 401 con token guardado (api.ts ya lo borró): vuelve a /login con el aviso.
  useEffect(
    () =>
      registrarSesionVencida(() => {
        setToken(null)
        setUsuario(null)
        aplicarPreferencias(null)
        navigate("/login", {
          replace: true,
          state: { aviso: MENSAJE_SESION_VENCIDA },
        })
      }),
    [navigate]
  )

  const login = useCallback<AuthContextValor["login"]>(
    async (nombreUsuario, contrasenia, mantenerSesion) => {
      const respuesta = await loginApi(nombreUsuario, contrasenia, mantenerSesion)
      guardarToken(respuesta.token, mantenerSesion)
      setToken(respuesta.token)
      setUsuario(respuesta.usuario)
      aplicarPreferencias(respuesta.usuario.preferenciasAccesibilidad)
      return respuesta.usuario
    },
    []
  )

  const logout = useCallback(() => {
    borrarToken()
    setToken(null)
    setUsuario(null)
    aplicarPreferencias(null)
  }, [])

  const valor = useMemo<AuthContextValor>(
    () => ({ token, usuario, rol: usuario?.rol ?? null, cargando, login, logout }),
    [token, usuario, cargando, login, logout]
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
