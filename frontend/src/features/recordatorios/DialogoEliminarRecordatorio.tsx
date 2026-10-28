import type { ReactElement } from "react"

import { DialogoConfirmacion } from "@/components/alma/dialogo-confirmacion"

import {
  ELIMINAR_AVISO,
  ELIMINAR_CANCELAR,
  ELIMINAR_CONFIRMAR,
  ELIMINAR_PREGUNTA,
} from "./mensajes"

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
    <DialogoConfirmacion
      titulo={ELIMINAR_PREGUNTA}
      texto={ELIMINAR_AVISO}
      confirmar={ELIMINAR_CONFIRMAR}
      cancelar={ELIMINAR_CANCELAR}
      variante="destructive"
      onConfirmar={onConfirmar}
    >
      {children}
    </DialogoConfirmacion>
  )
}
