import { useContext } from "react"

import { ChatContexto } from "./contexto"

export function useChat() {
  const contexto = useContext(ChatContexto)
  if (!contexto) {
    throw new Error("useChat tiene que usarse dentro de <ChatProvider>.")
  }
  return contexto
}
