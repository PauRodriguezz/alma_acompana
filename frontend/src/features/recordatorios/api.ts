// Depende de /api/recordatorios del contrato (docs/contrato-recordatorios.md, sección 6).
import { api } from "@/lib/api"

import type {
  Cita,
  DatosCita,
  DatosMedicacion,
  EstadoNotificacion,
  ItemAgenda,
  Medicacion,
  Recordatorio,
  TipoRecordatorio,
} from "./tipos"

const BASE = "/recordatorios"

/** 6.1: notificaciones entre dos fechas ("yyyy-MM-dd", inclusive; máximo 62 días). */
export function agenda(desde: string, hasta: string, tipo?: TipoRecordatorio): Promise<ItemAgenda[]> {
  const parametros = new URLSearchParams({ desde, hasta })
  if (tipo) parametros.set("tipo", tipo)
  return api<ItemAgenda[]>(`${BASE}/agenda?${parametros}`)
}

/** 6.8: todos los recordatorios de la persona, con filtro opcional por tipo. */
export function listar(tipo?: TipoRecordatorio): Promise<Recordatorio[]> {
  return api<Recordatorio[]>(tipo ? `${BASE}?tipo=${tipo}` : BASE)
}

/** 6.2 */
export function detalle(id: number): Promise<Recordatorio> {
  return api<Recordatorio>(`${BASE}/${id}`)
}

/** 6.3 */
export function crearMedicacion(datos: DatosMedicacion): Promise<Medicacion> {
  return api<Medicacion>(`${BASE}/medicacion`, { method: "POST", cuerpo: datos })
}

/** 6.4 */
export function crearCita(datos: DatosCita): Promise<Cita> {
  return api<Cita>(`${BASE}/citas`, { method: "POST", cuerpo: datos })
}

/** 6.5 */
export function editarMedicacion(id: number, datos: DatosMedicacion): Promise<Medicacion> {
  return api<Medicacion>(`${BASE}/medicacion/${id}`, { method: "PUT", cuerpo: datos })
}

/** 6.5 */
export function editarCita(id: number, datos: DatosCita): Promise<Cita> {
  return api<Cita>(`${BASE}/citas/${id}`, { method: "PUT", cuerpo: datos })
}

/** 6.6: borra el recordatorio y todos sus avisos. */
export function eliminar(id: number): Promise<void> {
  return api<void>(`${BASE}/${id}`, { method: "DELETE" })
}

/** 6.7: marca una toma como tomada o una cita como vista, o las vuelve a pendiente. */
export function cambiarEstado(notificacionId: number, estado: EstadoNotificacion): Promise<ItemAgenda> {
  return api<ItemAgenda>(`${BASE}/notificaciones/${notificacionId}/estado`, {
    method: "PUT",
    cuerpo: { estado },
  })
}
