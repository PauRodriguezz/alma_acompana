import { useParams } from "react-router-dom"

/** El `:id` de la ruta como número, o `undefined` si falta o no son solo dígitos. */
export function useIdDeRuta(): number | undefined {
  const { id } = useParams()
  return id !== undefined && /^\d+$/.test(id) ? Number(id) : undefined
}
