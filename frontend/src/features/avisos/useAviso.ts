import { useContext } from "react"

import { AvisoContext } from "./aviso-context"

export function useAviso() {
  const contexto = useContext(AvisoContext)
  if (!contexto) {
    throw new Error("useAviso tiene que usarse dentro de <AvisoProvider>.")
  }
  return contexto
}
