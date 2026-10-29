import type { ReactElement } from "react"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"

import { ELIMINAR_AVISO, ELIMINAR_CONFIRMAR, ELIMINAR_PREGUNTA } from "../mensajes"

/**
 * Confirmación antes de eliminar un recordatorio (CU-07 y CU-08; contrato, 8.1). Recibe como
 * hijo el botón que la abre. Al confirmar llama a `onConfirmar`: quien la usa se ocupa de borrar
 * y de avisar.
 */
export function DialogoEliminarRecordatorio({
  children,
  onConfirmar,
}: {
  children: ReactElement
  onConfirmar: () => void
}) {
  return (
    <DialogoConfirmar
      titulo={ELIMINAR_PREGUNTA}
      descripcion={ELIMINAR_AVISO}
      textoConfirmar={ELIMINAR_CONFIRMAR}
      variante="destructive"
      alConfirmar={onConfirmar}
      disparador={children}
    />
  )
}
