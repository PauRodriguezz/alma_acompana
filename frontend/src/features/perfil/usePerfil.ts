import { useEffect, useState } from "react"

import { obtenerPerfil, type Perfil } from "./api"

type EstadoPerfil =
  | { tipo: "cargando" }
  | { tipo: "listo"; perfil: Perfil }
  | { tipo: "error" }

/** Carga el perfil al montar. Si la pantalla se cierra antes de que llegue, ignora la respuesta. */
export function usePerfil(): EstadoPerfil {
  const [estado, setEstado] = useState<EstadoPerfil>({ tipo: "cargando" })

  useEffect(() => {
    let cancelado = false
    obtenerPerfil()
      .then((perfil) => {
        if (!cancelado) setEstado({ tipo: "listo", perfil })
      })
      .catch(() => {
        if (!cancelado) setEstado({ tipo: "error" })
      })
    return () => {
      cancelado = true
    }
  }, [])

  return estado
}
