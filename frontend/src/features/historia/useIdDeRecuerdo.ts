import { useParams } from "react-router-dom"

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * El `:recuerdoId` de la ruta como texto, o `undefined` si falta o no tiene formato de UUID
 * (contrato, 11.3). Con `undefined` el recuerdo se trata como no encontrado, sin llamar al back.
 */
export function useIdDeRecuerdo(): string | undefined {
  const { recuerdoId } = useParams()
  return recuerdoId !== undefined && UUID.test(recuerdoId) ? recuerdoId : undefined
}
