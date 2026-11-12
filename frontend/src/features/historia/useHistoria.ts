import { useQuery } from "@tanstack/react-query"

import { useAuth } from "@/features/auth/useAuth"

import { detalleDeRecuerdo, estadoDelGuion, listarRecuerdos } from "./api"

// Las consultas del módulo cuelgan de ["historia", idDelUsuario, ...]: así no se mezclan los
// datos de dos personas que usan el mismo navegador.
export const clavesHistoria = {
  raiz: (usuarioId: number | undefined) => ["historia", usuarioId] as const,
  recuerdos: (usuarioId: number | undefined) => ["historia", usuarioId, "recuerdos"] as const,
  detalle: (usuarioId: number | undefined, id: string | undefined) =>
    ["historia", usuarioId, "detalle", id] as const,
  estado: (usuarioId: number | undefined) => ["historia", usuarioId, "estado"] as const,
}

/** Línea de tiempo (7.3). */
export function useRecuerdos() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesHistoria.recuerdos(usuario?.id),
    queryFn: listarRecuerdos,
    enabled: usuario !== null,
  })
}

/** Detalle (7.4). Con un id inválido (undefined) no se pide nada. */
export function useRecuerdo(id: string | undefined) {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesHistoria.detalle(usuario?.id, id),
    queryFn: () => detalleDeRecuerdo(id as string),
    enabled: usuario !== null && id !== undefined,
  })
}

/** Cuánto vale el estado del guion sin pedirlo de nuevo: se refresca al invalidar tras responder o saltear. */
const VIGENCIA_DEL_ESTADO_MS = 30 * 60 * 1000

/** Estado del guion: dice si hay una pregunta pendiente (5.4 y botón fijo del chat, contrato del mapa de vida 2.8). */
export function useEstadoDelGuion() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesHistoria.estado(usuario?.id),
    queryFn: estadoDelGuion,
    enabled: usuario !== null,
    staleTime: VIGENCIA_DEL_ESTADO_MS,
    refetchOnWindowFocus: false,
  })
}
