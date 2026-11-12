import { useState } from "react"
import { useNavigate } from "react-router-dom"

import { useChat } from "@/features/chat/useChat"
import { mensajeDeError } from "@/lib/errores"

import { ALMA_TODAVIA_RESPONDE, SIN_MAS_PARA_PREGUNTAR } from "./mensajes"
import { RUTA_CHAT } from "./rutas"

/**
 * "Seguir completando este recuerdo" (contrato del mapa de vida, 2.7), compartido por el panel de la etapa y el
 * detalle. `completar` pide la repregunta del recuerdo al chat: con una, navega al chat; sin ninguna, deja el aviso
 * (`aviso`) en esta pantalla (la lista se actualiza sola y el botón puede desaparecer, así que el aviso no vive en él);
 * si el pedido falla, deja el mensaje en `error`.
 */
export function useSeguirCompletando() {
  const navegar = useNavigate()
  const { seguirCompletando, ocupadoGuion } = useChat()
  const [esperandoId, setEsperandoId] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function completar(recuerdoId: string) {
    if (esperandoId !== null || ocupadoGuion) return
    setEsperandoId(recuerdoId)
    setAviso(null)
    setError(null)
    try {
      const { resultado } = await seguirCompletando(recuerdoId)
      // Solo se navega si el chat dejó la repregunta: con YA_ACTIVA (el chat siguió ocupado) no hay nada para ver.
      if (resultado === "ABIERTA") navegar(RUTA_CHAT)
      else setAviso(resultado === "NINGUNA" ? SIN_MAS_PARA_PREGUNTAR : ALMA_TODAVIA_RESPONDE)
    } catch (e) {
      setError(mensajeDeError(e))
    } finally {
      setEsperandoId(null)
    }
  }

  return { completar, esperandoId, ocupado: esperandoId !== null || ocupadoGuion, aviso, error }
}
