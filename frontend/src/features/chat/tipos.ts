// Depende de /api/chat del contrato (docs/contrato-chat.md, secciones 3 y 7) y, para el modo guion,
// de docs/contrato-historia-de-vida.md (2.1 a 2.5 y 11.4).
import type { DatoFaltante } from "@/features/historia/tipos"

/** Quién escribió un turno (el back usa el enum en mayúsculas). */
export type RolChat = "PERSONA" | "ALMA"

/**
 * Qué es un mensaje de la conversación. `GUION`: una pregunta o repregunta del guion. `INVITACION` y
 * `CIERRE`: textos fijos del front, no los generó el modelo (no viajan como historial).
 */
export type TipoMensaje = "TEXTO" | "GUION" | "INVITACION" | "CIERRE"

/** Un mensaje de la conversación que se ve en pantalla. El `id` es solo de este navegador. */
export type MensajeChat = {
  id: number
  rol: RolChat
  texto: string
  /** Por defecto `TEXTO`. */
  tipo?: TipoMensaje
  /** La pregunta del guion, destacada debajo de la introducción (Figma 29:399). */
  preguntaDestacada?: string
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
  /** true si respondió el detector de riesgo (urgencia médica o malestar grave); false si respondió el modelo. */
  alarma: boolean
}

/** A qué apunta el campo de texto en modo guion (contrato de historia de vida, 2.1). `null` = chat normal. */
export type ObjetivoGuion =
  | { tipo: "PREGUNTA"; preguntaId: number }
  | { tipo: "REPREGUNTA"; recuerdoId: string; dato: DatoFaltante }

/** Qué pasó al pedir la próxima pregunta desde la pantalla de historia (5.4). */
export type ResultadoApertura =
  | { resultado: "ABIERTA" | "YA_ACTIVA" }
  | { resultado: "SIN_PENDIENTES"; mensajeFinal: string }

/** Largo máximo del mensaje (5.9): el campo lo corta en 500 caracteres. */
export const LIMITE_MENSAJE = 500

/** En modo guion el campo admite 1000, lo que acepta el back para una respuesta del guion (5.9 del contrato de historia). */
export const LIMITE_RESPUESTA_GUION = 1000

/** Cuántos turnos anteriores se mandan con cada mensaje (sección 2). */
export const LIMITE_TURNOS = 10

/** El back espera hasta 10 s un turno y 30 s al modelo: el front espera un poco más (11.5). */
export const TIEMPO_MAXIMO_ENVIO_MS = 45_000

/** Cada cuánto se consulta si hay repregunta mientras el back procesa la respuesta (2.3). */
export const INTERVALO_ESPERA_MS = 2_000

/** Tope de la espera de la extracción: pasado este tiempo se sigue sin repregunta (2.3). */
export const TOPE_ESPERA_MS = 60_000
