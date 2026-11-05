import { createContext } from "react"

import type { RegistroAnimo } from "@/features/bienestar"

import type { MensajeChat } from "./tipos"

/** Todo lo que la pantalla del chat lee y puede hacer (contrato, 11.1). Vive en memoria. */
export type ChatContextoValor = {
  /** El ánimo elegido hoy (con su id y su pregunta contextual), o nada. */
  animo: RegistroAnimo | null
  /** La persona tocó "Cambiar cómo me siento" y está eligiendo de nuevo. */
  cambiandoAnimo: boolean
  /** Pasa a true cuando manda su primer mensaje y no vuelve a false. */
  yaEscribio: boolean
  mensajes: MensajeChat[]
  /** Se está esperando la respuesta de Alma. */
  pendiente: boolean
  /** Texto del error del último envío, o nada. */
  error: string | null
  /** Texto del campo de mensaje (se conserva al cambiar de pantalla). */
  borrador: string
  /** Aviso para el lector de pantalla cuando se registra o corrige el ánimo. */
  anuncioAnimo: string
  cambiarBorrador: (texto: string) => void
  /** Manda un mensaje. Sin texto (sin contar espacios) o con otro envío en curso, no hace nada. */
  enviar: (texto: string) => void
  /** Vuelve a mandar el último mensaje que falló. */
  reintentar: () => void
  /** El widget registró o corrigió el ánimo. */
  alRegistrarAnimo: (registro: RegistroAnimo) => void
  empezarACambiarAnimo: () => void
}

export const ChatContexto = createContext<ChatContextoValor | null>(null)
