import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"

import {
  enviarACuenta,
  hubieronCambios,
  marcarTocadas,
  obtenerPreferencias,
  usarPreferencias,
} from "@/features/accesibilidad/almacen"
import { MENSAJE_SESION_VENCIDA, registrarSesionVencida } from "@/lib/api"
import { borrarToken, guardarToken, leerToken } from "@/lib/token"

import { login as loginApi, me, type Usuario } from "./api"
import { AuthContext, type AuthContextValor } from "./auth-context"

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const clienteConsultas = useQueryClient()
  // El token se lee una sola vez al montar: de esa lectura salen tanto `cargando` como la
  // decisión de consultar /api/auth/me, así que nunca pueden contradecirse.
  const [tokenInicial] = useState(() => leerToken())
  const [token, setToken] = useState<string | null>(tokenInicial)
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  // Sin token no hay nada que recuperar: cargando arranca en false y se va directo a /login.
  // Con token hay que consultar /api/auth/me antes de saber quién es.
  const [cargando, setCargando] = useState(tokenInicial !== null)

  // Recupera la sesión al abrir la app; ante cualquier falla (también sin conexión) descarta
  // el token, como dice el contrato. api() corta a los 10 s, así que el finally siempre llega.
  useEffect(() => {
    if (tokenInicial === null) return
    let cancelado = false
    me()
      .then((u) => {
        if (cancelado) return
        setUsuario(u)
        // CU-13 / contrato: al recuperar la sesión se aplican las preferencias de la cuenta
        // y se actualiza la copia del dispositivo. Un ADMINISTRADOR las trae en null: se
        // deja lo que tenga el dispositivo.
        if (u.preferenciasAccesibilidad) usarPreferencias(u.preferenciasAccesibilidad)
      })
      .catch(() => {
        // La pasada cancelada (StrictMode en desarrollo) no toca la sesión de la vigente.
        if (cancelado) return
        borrarToken()
        setToken(null)
      })
      .finally(() => {
        if (!cancelado) setCargando(false)
      })
    return () => {
      cancelado = true
    }
  }, [tokenInicial])

  // Único lugar donde se cierra la sesión (persona o administrador, a pedido o por un 401).
  const logout = useCallback(() => {
    borrarToken()
    setToken(null)
    setUsuario(null)
    // Las preferencias no vuelven a los valores por defecto: queda lo del dispositivo.
    marcarTocadas(false)
    // Se borran los datos guardados: la próxima cuenta que entre no ve datos de la anterior.
    clienteConsultas.clear()
  }, [clienteConsultas])

  // Un 401 con token guardado (api.ts ya lo borró): vuelve a /login con el aviso.
  useEffect(
    () =>
      registrarSesionVencida(() => {
        logout()
        navigate("/login", {
          replace: true,
          state: { aviso: MENSAJE_SESION_VENCIDA },
        })
      }),
    [logout, navigate]
  )

  const login = useCallback<AuthContextValor["login"]>(
    async (nombreUsuario, contrasenia, mantenerSesion) => {
      const respuesta = await loginApi(nombreUsuario, contrasenia, mantenerSesion)
      guardarToken(respuesta.token, mantenerSesion)
      setToken(respuesta.token)
      setUsuario(respuesta.usuario)
      // Accesibilidad al ingresar (solo PERSONA_MAYOR; el ADMINISTRADOR no tiene preferencias
      // en la cuenta y no se toca lo del dispositivo):
      // - si la persona tocó las opciones en esta visita, se mantiene lo que eligió y se
      //   manda a su cuenta sin bloquear el ingreso (si el PUT falla, queda en el dispositivo);
      // - si no, se aplican las de la cuenta y se actualiza la copia del dispositivo.
      if (respuesta.usuario.rol === "PERSONA_MAYOR") {
        if (hubieronCambios()) {
          enviarACuenta(obtenerPreferencias())
        } else if (respuesta.usuario.preferenciasAccesibilidad) {
          usarPreferencias(respuesta.usuario.preferenciasAccesibilidad)
        }
      }
      marcarTocadas(false)
      return respuesta.usuario
    },
    []
  )

  const valor = useMemo<AuthContextValor>(
    () => ({ token, usuario, rol: usuario?.rol ?? null, cargando, login, logout }),
    [token, usuario, cargando, login, logout]
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
