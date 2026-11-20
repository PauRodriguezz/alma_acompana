import { createContext } from "react"

export type AvisoValor = {
  /** Muestra un aviso de éxito flotante. El último reemplaza al anterior. */
  avisar: (texto: string) => void
}

export const AvisoContext = createContext<AvisoValor | null>(null)
