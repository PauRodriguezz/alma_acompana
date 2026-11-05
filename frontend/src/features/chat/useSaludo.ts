import { useQuery } from "@tanstack/react-query"

import { useAuth } from "@/features/auth/useAuth"

import { obtenerSaludo } from "./api"

/**
 * El saludo inicial de Alma (CU-02). No se vuelve a pedir al enfocar la ventana ni al reconectar, y
 * no se descarta de la caché por inactividad: el texto no cambia durante la sesión (11.1). La clave
 * lleva el id de la persona para no mezclar sesiones.
 */
export function useSaludo() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: ["chat", usuario?.id, "saludo"],
    queryFn: obtenerSaludo,
    staleTime: Infinity,
    gcTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}
