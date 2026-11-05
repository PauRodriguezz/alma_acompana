import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { useAuth } from "@/features/auth/useAuth"

import { detalle, guardadas, guardar, listar, quitar } from "./api"
import type { LecturaDetalle } from "./tipos"

// Las consultas del módulo cuelgan de ["lecturas", idDelUsuario, ...]: así no se mezclan los
// datos de dos personas que usan el mismo navegador.
export const clavesLecturas = {
  raiz: (usuarioId: number | undefined) => ["lecturas", usuarioId] as const,
  todas: (usuarioId: number | undefined) => ["lecturas", usuarioId, "todas"] as const,
  guardadas: (usuarioId: number | undefined) => ["lecturas", usuarioId, "guardadas"] as const,
  detalle: (usuarioId: number | undefined, id: number | undefined) =>
    ["lecturas", usuarioId, "detalle", id] as const,
}

/** Pestaña "Todas" (6.1). */
export function useLecturas() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesLecturas.todas(usuario?.id),
    queryFn: listar,
    enabled: usuario !== null,
  })
}

/** Pestaña "Guardadas" (6.1). */
export function useGuardadas() {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesLecturas.guardadas(usuario?.id),
    queryFn: guardadas,
    enabled: usuario !== null,
  })
}

/** Detalle (6.2). Con un id inválido (undefined) no se pide nada. */
export function useLectura(id: number | undefined) {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: clavesLecturas.detalle(usuario?.id, id),
    queryFn: () => detalle(id as number),
    enabled: usuario !== null && id !== undefined,
  })
}

/**
 * Guardar (`guardar: true`) o quitar de guardadas (6.3). Al terminar bien (contrato, 10.3):
 * se cambia `guardada` en la copia local del detalle y se invalidan solo el listado y las
 * guardadas (con `exact`). El detalle no se vuelve a pedir: si la microcápsula estaba dada de
 * baja, un nuevo pedido daría 404 justo después de quitarla (sección 2).
 */
export function useCambiarGuardada() {
  const { usuario } = useAuth()
  const clienteConsultas = useQueryClient()
  return useMutation({
    mutationFn: ({ id, guardar: alGuardar }: { id: number; guardar: boolean }) =>
      alGuardar ? guardar(id) : quitar(id),
    onSuccess: (_, { id, guardar: alGuardar }) => {
      clienteConsultas.setQueryData<LecturaDetalle>(
        clavesLecturas.detalle(usuario?.id, id),
        (anterior) => (anterior ? { ...anterior, guardada: alGuardar } : anterior)
      )
      clienteConsultas.invalidateQueries({ queryKey: clavesLecturas.todas(usuario?.id), exact: true })
      clienteConsultas.invalidateQueries({
        queryKey: clavesLecturas.guardadas(usuario?.id),
        exact: true,
      })
    },
  })
}
