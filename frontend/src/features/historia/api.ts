import { api } from "@/lib/api"

import type {
  DatoFaltante,
  EstadoDelGuion,
  ListaDeRecuerdos,
  ProximaPregunta,
  RecuerdoDetalle,
  RepreguntaSiguiente,
  RespuestaDelGuion,
} from "./tipos"

const BASE = "/historia"

/** 7.3: ya vienen en orden cronológico (5.1); los que no tienen año, al final. */
export function listarRecuerdos(): Promise<ListaDeRecuerdos> {
  return api<ListaDeRecuerdos>(`${BASE}/recuerdos`)
}

/** 7.4: 404 si no existe o es de otra persona. */
export function detalleDeRecuerdo(id: string): Promise<RecuerdoDetalle> {
  return api<RecuerdoDetalle>(`${BASE}/recuerdos/${id}`)
}

/** Contrato del grafo, 7.2: hay preguntas pendientes si `pendientes` > 0. */
export function estadoDelGuion(): Promise<EstadoDelGuion> {
  return api<EstadoDelGuion>(`${BASE}/guion/estado`)
}

// ── Guion y repreguntas ──

/** Contrato del grafo, 7.1: la pregunta pendiente de menor orden con su introducción. */
export function proximaPregunta(): Promise<ProximaPregunta> {
  return api<ProximaPregunta>(`${BASE}/guion/proxima`)
}

/** 7.1: 202 ACEPTADA (empieza la espera) o 200 ALARMA (no se guardó nada). */
export function responderPregunta(preguntaId: number, texto: string): Promise<RespuestaDelGuion> {
  return api<RespuestaDelGuion>(`${BASE}/guion/${preguntaId}/respuesta`, {
    method: "POST",
    cuerpo: { texto },
  })
}

/** Contrato del grafo, 7.4: 204. */
export function saltearPregunta(preguntaId: number): Promise<void> {
  return api<void>(`${BASE}/guion/${preguntaId}/salteo`, { method: "POST" })
}

/** 7.2: igual que `responderPregunta`, para una repregunta. */
export function responderRepregunta(
  recuerdoId: string,
  dato: DatoFaltante,
  texto: string
): Promise<RespuestaDelGuion> {
  return api<RespuestaDelGuion>(`${BASE}/repreguntas/${recuerdoId}/${dato}/respuesta`, {
    method: "POST",
    cuerpo: { texto },
  })
}

/** Contrato del grafo, 7.7: 204. */
export function saltearRepregunta(recuerdoId: string, dato: DatoFaltante): Promise<void> {
  return api<void>(`${BASE}/repreguntas/${recuerdoId}/${dato}/salteo`, { method: "POST" })
}

/**
 * Contrato del grafo, 7.5, y del mapa de vida, 3.3. Con `preguntaId` (la pregunta que dio origen a la charla) se
 * repregunta solo sobre los recuerdos de esa pregunta; con `recuerdoId` (seguir completando un recuerdo), solo sobre
 * ese recuerdo; sin ninguno, el back busca en todos. No se mandan los dos a la vez (400). `excluir` son los datos ya
 * preguntados en esta charla.
 */
export type OrigenDeRepregunta = { preguntaId: number } | { recuerdoId: string } | null

export function siguienteRepregunta(
  origen: OrigenDeRepregunta,
  excluir: DatoFaltante[]
): Promise<RepreguntaSiguiente> {
  const parametros = new URLSearchParams()
  if (origen && "preguntaId" in origen) parametros.set("preguntaId", String(origen.preguntaId))
  if (origen && "recuerdoId" in origen) parametros.set("recuerdoId", origen.recuerdoId)
  if (excluir.length > 0) parametros.set("excluir", excluir.join(","))
  const consulta = parametros.toString()
  return api<RepreguntaSiguiente>(`${BASE}/repreguntas/siguiente${consulta ? `?${consulta}` : ""}`)
}
