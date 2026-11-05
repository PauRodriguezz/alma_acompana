import { api } from "@/lib/api"

import {
  TIEMPO_MAXIMO_ENVIO_MS,
  type RespuestaChat,
  type SaludoChat,
  type TurnoChat,
} from "./tipos"

/** GET /api/chat/saludo (7.1). */
export function obtenerSaludo(): Promise<SaludoChat> {
  return api<SaludoChat>("/chat/saludo")
}

/**
 * POST /api/chat/mensajes (7.2). La respuesta llega completa. Espera hasta 45 s (el modelo puede
 * tardar) y quien la llama no reintenta: un segundo pedido sería otra consulta al modelo.
 */
export function enviarMensaje(mensaje: string, conversacion: TurnoChat[]): Promise<RespuestaChat> {
  return api<RespuestaChat>("/chat/mensajes", {
    method: "POST",
    cuerpo: { mensaje, conversacion },
    tiempoMaximoMs: TIEMPO_MAXIMO_ENVIO_MS,
  })
}
