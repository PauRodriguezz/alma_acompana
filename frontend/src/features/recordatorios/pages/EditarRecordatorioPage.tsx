import { useState } from "react"
import { useNavigate } from "react-router-dom"

import { EnlaceVolver } from "@/components/EnlaceVolver"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"
import { ESTADO_CARGANDO } from "@/lib/mensajes"
import { useIdDeRuta } from "@/lib/useIdDeRuta"

import { DialogoEliminarRecordatorio } from "../components/DialogoEliminarRecordatorio"
import { FormularioCita } from "../components/FormularioCita"
import { FormularioMedicacion } from "../components/FormularioMedicacion"
import {
  AVISO_ELIMINADO,
  BOTON_ELIMINAR_RECORDATORIO,
  TITULO_EDITAR,
  VOLVER_A_RECORDATORIOS,
} from "../mensajes"
import { useDetalle, useEliminar } from "../useRecordatorios"

/** Edición (CU-07 y CU-08): el tipo sale del recordatorio cargado. Desde acá también se elimina. */
export function EditarRecordatorioPage() {
  const id = useIdDeRuta()
  const navigate = useNavigate()
  const { avisar } = useAviso()
  const consulta = useDetalle(id)
  const eliminar = useEliminar()
  const [errorEliminar, setErrorEliminar] = useState<unknown>(null)

  function borrar() {
    if (id === undefined) return
    setErrorEliminar(null)
    eliminar.mutate(id, {
      onSuccess: () => {
        avisar(AVISO_ELIMINADO)
        navigate("/recordatorios")
      },
      onError: (error) => setErrorEliminar(error),
    })
  }

  return (
    <div className="mx-auto flex w-full max-w-(--narrow-max) flex-col gap-4 lg:mx-0">
      <EnlaceVolver to="/recordatorios" texto={VOLVER_A_RECORDATORIOS} />
      <h1>{TITULO_EDITAR}</h1>

      {id === undefined ? <Alert variant="error">{mensajeDeError(new ApiError(404))}</Alert> : null}
      {consulta.isPending && id !== undefined ? (
        <p role="status" className="text-muted-foreground">
          {ESTADO_CARGANDO}
        </p>
      ) : null}
      {consulta.error ? <Alert variant="error">{mensajeDeError(consulta.error)}</Alert> : null}
      {errorEliminar ? <Alert variant="error">{mensajeDeError(errorEliminar)}</Alert> : null}

      {consulta.data ? (
        <>
          {consulta.data.tipo === "MEDICACION" ? (
            <FormularioMedicacion medicacion={consulta.data} />
          ) : (
            <FormularioCita cita={consulta.data} />
          )}
          <DialogoEliminarRecordatorio onConfirmar={borrar}>
            <Button variant="outline" disabled={eliminar.isPending} className="w-full">
              {BOTON_ELIMINAR_RECORDATORIO}
            </Button>
          </DialogoEliminarRecordatorio>
        </>
      ) : null}
    </div>
  )
}
