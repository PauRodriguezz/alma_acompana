import { useMutation, useQuery } from "@tanstack/react-query"

import { useAuth } from "@/features/auth/useAuth"

import { enviarRespuesta, obtenerSiguiente } from "./api"
import type { RespuestaCuerpo } from "./tipos"

// Las consultas del módulo cuelgan de ["juegos", idDelUsuario, ...]: así no se mezclan los
// datos de dos personas que usan el mismo navegador.
export const clavesJuegos = {
  raiz: (usuarioId: number | undefined) => ["juegos", usuarioId] as const,
  siguiente: (usuarioId: number | undefined) => ["juegos", usuarioId, "siguiente"] as const,
}

/**
 * Próximo ejercicio (6.1). Se pide siempre de nuevo (sin caché) y nunca por su cuenta: si se
 * volviera a pedir al volver a la ventana o al reconectar, el ejercicio cambiaría mientras la
 * persona lo está mirando (contrato, 10.3). "Continuar" y "Seguir jugando" llaman a refetch().
 */
export function useSiguienteEjercicio() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesJuegos.siguiente(usuario?.id),
    queryFn: obtenerSiguiente,
    enabled: usuario !== null,
    staleTime: 0,
    gcTime: 0,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
}

/** Respuesta a un ejercicio (6.2). No reintenta: un segundo envío registraría otro intento. */
export function useResponder() {
  return useMutation({
    mutationFn: (cuerpo: RespuestaCuerpo) => enviarRespuesta(cuerpo),
  })
}
