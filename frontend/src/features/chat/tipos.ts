// Depende de /api/chat del contrato (docs/contrato-chat.md, secciones 3 y 7).

/** Quién escribió un turno (el back usa el enum en mayúsculas). */
export type RolChat = "PERSONA" | "ALMA"

/** Un mensaje de la conversación que se ve en pantalla. El `id` es solo de este navegador. */
export type MensajeChat = {
  id: number
  rol: RolChat
  texto: string
}

/** Un turno anterior tal como viaja en `conversacion` (7.2). */
export type TurnoChat = {
  rol: RolChat
  texto: string
}

/** Lo que devuelve GET /api/chat/saludo (7.1). */
export type SaludoChat = {
  saludo: string
  mensaje: string
}

/** Lo que devuelve POST /api/chat/mensajes (7.2). */
export type RespuestaChat = {
  respuesta: string
}

/** Largo máximo del mensaje (5.9): el campo lo corta en 500 caracteres. */
export const LIMITE_MENSAJE = 500

/** Cuántos turnos anteriores se mandan con cada mensaje (sección 2). */
export const LIMITE_TURNOS = 10

/** El back espera hasta 10 s un turno y 30 s al modelo: el front espera un poco más (11.5). */
export const TIEMPO_MAXIMO_ENVIO_MS = 45_000
