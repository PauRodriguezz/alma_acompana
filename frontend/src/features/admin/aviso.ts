import { useCallback, useEffect, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"

export type Aviso = { texto: string; clave: number }

/** Estado de navegación con el que un formulario deja el aviso de éxito para la lista. */
export function conAviso(texto: string) {
  return { state: { aviso: texto } }
}

/**
 * Aviso de éxito de una lista del panel. Toma el que llega al navegar desde el formulario
 * (navigate(ruta, conAviso("…"))) y lo borra del historial, para que no vuelva a aparecer
 * al recargar. mostrar() pone uno nuevo (por ejemplo después de una baja). La clave hace
 * que dos avisos iguales seguidos se anuncien los dos.
 */
export function useAvisoExito(): [Aviso | null, (texto: string) => void] {
  const location = useLocation()
  const navigate = useNavigate()
  const textoInicial = (location.state as { aviso?: string } | null)?.aviso
  const [aviso, setAviso] = useState<Aviso | null>(() =>
    textoInicial ? { texto: textoInicial, clave: 1 } : null
  )

  useEffect(() => {
    if (textoInicial) {
      navigate(`${location.pathname}${location.search}`, { replace: true, state: null })
    }
  }, [textoInicial, location.pathname, location.search, navigate])

  const mostrar = useCallback(
    (texto: string) => setAviso((anterior) => ({ texto, clave: (anterior?.clave ?? 0) + 1 })),
    []
  )

  return [aviso, mostrar]
}
