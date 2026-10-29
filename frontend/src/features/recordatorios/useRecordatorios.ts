import { useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { format } from "date-fns"

import { useAuth } from "@/features/auth/useAuth"
import { useAhora } from "@/lib/useAhora"

import {
  agenda,
  cambiarEstado,
  crearCita,
  crearMedicacion,
  detalle,
  editarCita,
  editarMedicacion,
  eliminar,
  listar,
} from "./api"
import { anuncioDeshecha, anuncioTomada } from "./mensajes"
import type {
  DatosCita,
  DatosMedicacion,
  EstadoNotificacion,
  ItemAgenda,
  TipoRecordatorio,
} from "./tipos"

// Todas las consultas del módulo cuelgan de ["recordatorios", idDelUsuario, ...]: así no se
// mezclan los datos de dos personas que usan el mismo navegador, y después de crear, editar,
// eliminar o cambiar un estado se invalida todo de una vez (agenda, listado y detalle).
function raiz(usuarioId: number | undefined) {
  return ["recordatorios", usuarioId] as const
}

/** Notificaciones entre dos fechas "yyyy-MM-dd" (inclusive). Ver 6.1 del contrato. */
export function useAgenda(
  desde: string,
  hasta: string,
  tipo?: TipoRecordatorio,
  opciones: { refetchInterval?: number } = {}
) {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: [...raiz(usuario?.id), "agenda", desde, hasta, tipo ?? "TODOS"],
    queryFn: () => agenda(desde, hasta, tipo),
    enabled: usuario !== null,
    // La agenda lleva estados que cambian con el paso del tiempo: al volver a la ventana se
    // vuelve a pedir (el cliente de consultas, por defecto, no lo hace).
    refetchOnWindowFocus: true,
    ...opciones,
  })
}

/**
 * Agenda de hoy (en hora de Argentina): se vuelve a pedir cada 60 segundos y al volver a la
 * ventana. Es la base del aviso "Para hoy" (5.7) y de "Medicación de hoy"; el módulo de Chat la
 * reutiliza para armar su tarjeta.
 */
export function useAgendaDeHoy() {
  const hoy = format(useAhora(), "yyyy-MM-dd")
  return useAgenda(hoy, hoy, undefined, { refetchInterval: 60_000 })
}

/** Todos los recordatorios de la persona (6.8), con filtro opcional por tipo. */
export function useRecordatoriosLista(tipo?: TipoRecordatorio) {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: [...raiz(usuario?.id), "lista", tipo ?? "TODOS"],
    queryFn: () => listar(tipo),
    enabled: usuario !== null,
  })
}

/** Un recordatorio por id (6.2). Una persona ajena o un id inexistente dan 404. */
export function useDetalle(id: number | undefined) {
  const { usuario } = useAuth()
  return useQuery({
    queryKey: [...raiz(usuario?.id), "detalle", id],
    queryFn: () => detalle(id as number),
    enabled: usuario !== null && id !== undefined,
  })
}

// Después de crear, editar o eliminar, se invalida todo lo del módulo (agenda, listado y detalle).
function useInvalidarTodo() {
  const clienteConsultas = useQueryClient()
  const { usuario } = useAuth()
  return () => clienteConsultas.invalidateQueries({ queryKey: raiz(usuario?.id) })
}

/** Alta (sin id) o edición (con id) de una medicación (6.3 y 6.5). */
export function useGuardarMedicacion(id?: number) {
  const invalidar = useInvalidarTodo()
  return useMutation({
    mutationFn: (datos: DatosMedicacion) =>
      id === undefined ? crearMedicacion(datos) : editarMedicacion(id, datos),
    onSuccess: invalidar,
  })
}

/** Alta (sin id) o edición (con id) de una cita (6.4 y 6.5). */
export function useGuardarCita(id?: number) {
  const invalidar = useInvalidarTodo()
  return useMutation({
    mutationFn: (datos: DatosCita) => (id === undefined ? crearCita(datos) : editarCita(id, datos)),
    onSuccess: invalidar,
  })
}

/** Baja de un recordatorio y de todos sus avisos (6.6). */
export function useEliminar() {
  const clienteConsultas = useQueryClient()
  const { usuario } = useAuth()
  return useMutation({
    mutationFn: (id: number) => eliminar(id),
    // Se invalida todo menos el detalle del recordatorio borrado: volver a pedirlo daría 404.
    onSuccess: (_resultado, id) =>
      clienteConsultas.invalidateQueries({
        queryKey: raiz(usuario?.id),
        predicate: (consulta) => !(consulta.queryKey[2] === "detalle" && consulta.queryKey[3] === id),
      }),
  })
}

/** Marcar una toma como tomada o una cita como vista, o volverlas a pendiente (6.7). */
export function useCambiarEstado() {
  const clienteConsultas = useQueryClient()
  const { usuario } = useAuth()
  return useMutation({
    mutationFn: (datos: { notificacionId: number; estado: EstadoNotificacion }) =>
      cambiarEstado(datos.notificacionId, datos.estado),
    onSuccess: () => clienteConsultas.invalidateQueries({ queryKey: raiz(usuario?.id) }),
  })
}

/**
 * Marcar una toma como tomada o deshacerla, desde la Lista o desde el detalle de un día del
 * Calendario. Además del cambio, deja el aviso para el lector de pantalla (`anuncio`), el
 * error si falló y el id del aviso que se está guardando (para deshabilitar su botón).
 */
export function useCambioDeEstado() {
  const cambiarEstado = useCambiarEstado()
  const [anuncio, setAnuncio] = useState("")
  const [error, setError] = useState<unknown>(null)

  function cambiar(item: ItemAgenda, estado: EstadoNotificacion) {
    const nombre = item.recordatorio.tipo === "MEDICACION" ? item.recordatorio.nombreMedicamento : ""
    setError(null)
    cambiarEstado.mutate(
      { notificacionId: item.notificacionId, estado },
      {
        onSuccess: () => setAnuncio(estado === "TOMADA" ? anuncioTomada(nombre) : anuncioDeshecha(nombre)),
        onError: (falla) => setError(falla),
      }
    )
  }

  const ocupadoId = cambiarEstado.isPending ? cambiarEstado.variables?.notificacionId : undefined

  return { cambiar, ocupadoId, anuncio, error }
}
