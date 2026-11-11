import { createContext, type RefObject } from "react"

import type { RegistroAnimo } from "@/features/bienestar"

import type { MensajeChat, ObjetivoGuion, ResultadoApertura } from "./tipos"

/** Todo lo que la pantalla del chat lee y puede hacer (contrato, 11.1). Vive en memoria. */
export type ChatContextoValor = {
  /** El ánimo elegido hoy (con su id y su pregunta contextual), o nada. */
  animo: RegistroAnimo | null
  /** La persona tocó "Cambiar cómo me siento" y está eligiendo de nuevo. */
  cambiandoAnimo: boolean
  /** Pasa a true cuando manda su primer mensaje y no vuelve a false. */
  yaEscribio: boolean
  mensajes: MensajeChat[]
  /** Se está esperando algo de Alma: la respuesta de un mensaje o la repregunta del guion. */
  pendiente: boolean
  /** Texto del error del último envío, o nada. */
  error: string | null
  /** El error se puede reintentar con `reintentar` ("Probar de nuevo"). */
  errorReintentable: boolean
  /** Texto del campo de mensaje (se conserva al cambiar de pantalla). */
  borrador: string
  /** Aviso para el lector de pantalla cuando se registra o corrige el ánimo. */
  anuncioAnimo: string
  cambiarBorrador: (texto: string) => void
  /** Manda un mensaje (al guion si hay un objetivo activo). Sin texto o con otro envío en curso, no hace nada. */
  enviar: (texto: string) => void
  /** Vuelve a hacer lo último que falló, en el mismo modo (chat o guion). */
  reintentar: () => void
  /** El widget registró o corrigió el ánimo. */
  alRegistrarAnimo: (registro: RegistroAnimo) => void
  empezarACambiarAnimo: () => void

  // ── Modo guion (contrato de historia de vida, 2.1 a 2.5 y 11.4) ──
  /** A qué apunta el campo de texto: una pregunta, una repregunta o nada (chat normal). */
  objetivoActivo: ObjetivoGuion | null
  /** Se mostró el cierre "¿Querés contarme algo más…?" y faltan sus dos botones. */
  cierrePendiente: boolean
  /** La invitación "¿Seguimos completando tu historia de vida?" espera un "Sí" o un "Ahora no". */
  invitacionPendiente: boolean
  /** Hay un pedido del guion en curso: los botones quedan deshabilitados. */
  ocupadoGuion: boolean
  /** Pide la próxima pregunta y la deja en el chat. La usa la tarjeta "Próxima parada". Lanza si el pedido falla. */
  abrirPreguntaDelGuion: () => Promise<ResultadoApertura>
  aceptarInvitacion: () => void
  rechazarInvitacion: () => void
  /** Con una pregunta en pantalla: la saltea y pide la siguiente. */
  otraPregunta: () => void
  /** Con el cierre en pantalla: pide la siguiente (la anterior ya está respondida: no hay salteo). */
  otraPreguntaDespuesDelCierre: () => void
  /** Del cierre: vuelve a conversar normal. */
  contarAlgoMas: () => void
  saltearRepregunta: () => void
  /** Navega a Historia de vida y sale del modo guion; la pregunta sigue pendiente en el back. */
  volverAHistoria: () => void
  /** El campo de mensaje: `CampoMensaje` lo usa como `ref` y los botones del guion le devuelven el foco (11.7). */
  campoDeMensaje: RefObject<HTMLInputElement | null>
  enfocarCampo: () => void
}

export const ChatContexto = createContext<ChatContextoValor | null>(null)
