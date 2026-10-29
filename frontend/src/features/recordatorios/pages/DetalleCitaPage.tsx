import { useEffect, useRef, useState } from "react"
import { Clock01Icon, ViewIcon } from "@hugeicons/core-free-icons"
import { Link, Navigate, useNavigate } from "react-router-dom"

import { Chip } from "@/components/alma/chip"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"
import { useIdDeRuta } from "@/lib/useIdDeRuta"
import { ACCION_ELIMINAR, ESTADO_CARGANDO } from "@/lib/mensajes"

import { DialogoEliminarRecordatorio } from "../components/DialogoEliminarRecordatorio"
import { EnlaceVolver } from "../components/EnlaceVolver"
import { fechaCompleta, horaDeFechaHora } from "../fechas"
import {
  AVISO_ELIMINADO,
  BOTON_EDITAR,
  CHIP_CITA,
  CHIP_PENDIENTE,
  CHIP_VISTA,
  ETIQUETA_ESPECIALIDAD,
  ETIQUETA_INDICACIONES_DETALLE,
  ETIQUETA_LUGAR,
  ETIQUETA_MEDICO,
} from "../mensajes"
import { useCambiarEstado, useDetalle, useEliminar } from "../useRecordatorios"

function Dato({ etiqueta, children }: { etiqueta: string; children: string }) {
  return (
    <div className="flex flex-col gap-1 border-b-2 border-border py-3">
      <dt className="text-muted-foreground">{etiqueta}</dt>
      <dd className="font-bold break-words">{children}</dd>
    </div>
  )
}

/**
 * Detalle de una cita (CU-09). Al abrirla, si está PENDIENTE, se marca como vista (6.7).
 * Desde acá se edita y se elimina.
 */
export function DetalleCitaPage() {
  const id = useIdDeRuta()
  const navigate = useNavigate()
  const { avisar } = useAviso()
  const consulta = useDetalle(id)
  const cambiarEstado = useCambiarEstado()
  const eliminar = useEliminar()
  const [errorEliminar, setErrorEliminar] = useState<unknown>(null)

  // Se marca una sola vez por apertura, aunque la pantalla se dibuje de nuevo.
  const yaMarcada = useRef(false)
  const recordatorio = consulta.data
  const { mutate: marcar } = cambiarEstado
  useEffect(() => {
    if (recordatorio?.tipo === "CITA" && recordatorio.estado === "PENDIENTE" && !yaMarcada.current) {
      yaMarcada.current = true
      marcar({ notificacionId: recordatorio.notificacionId, estado: "VISTA" })
    }
  }, [recordatorio, marcar])

  // Una medicación no tiene detalle: se la lleva a su edición.
  if (recordatorio?.tipo === "MEDICACION") {
    return <Navigate to={`/recordatorios/${recordatorio.id}/editar`} replace />
  }

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
      <EnlaceVolver />

      {id === undefined ? <Alert variant="error">{mensajeDeError(new ApiError(404))}</Alert> : null}
      {consulta.isPending && id !== undefined ? (
        <p role="status" className="text-muted-foreground">
          {ESTADO_CARGANDO}
        </p>
      ) : null}
      {consulta.error ? <Alert variant="error">{mensajeDeError(consulta.error)}</Alert> : null}
      {errorEliminar ? <Alert variant="error">{mensajeDeError(errorEliminar)}</Alert> : null}

      {recordatorio?.tipo === "CITA" ? (
        <>
          <div className="flex flex-col gap-3">
            <div className="flex flex-wrap gap-2">
              <Chip tono="acento">{CHIP_CITA}</Chip>
              {recordatorio.estado === "VISTA" ? (
                <Chip tono="info" icono={ViewIcon}>
                  {CHIP_VISTA}
                </Chip>
              ) : (
                <Chip icono={Clock01Icon}>{CHIP_PENDIENTE}</Chip>
              )}
            </div>
            <h1 className="break-words">{recordatorio.titulo}</h1>
            <p className="font-heading text-h3 font-medium text-heading">
              {fechaCompleta(recordatorio.fechaHora)} · {horaDeFechaHora(recordatorio.fechaHora)}
            </p>
          </div>

          <dl>
            <Dato etiqueta={ETIQUETA_ESPECIALIDAD}>{recordatorio.especialidad}</Dato>
            <Dato etiqueta={ETIQUETA_MEDICO}>{recordatorio.medico}</Dato>
            <Dato etiqueta={ETIQUETA_LUGAR}>{recordatorio.lugar}</Dato>
            {recordatorio.indicacionesPrevias ? (
              <Dato etiqueta={ETIQUETA_INDICACIONES_DETALLE}>{recordatorio.indicacionesPrevias}</Dato>
            ) : null}
          </dl>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              to={`/recordatorios/${recordatorio.id}/editar`}
              className={buttonVariants({ className: "flex-1" })}
            >
              {BOTON_EDITAR}
            </Link>
            <DialogoEliminarRecordatorio onConfirmar={borrar}>
              <Button variant="outline" disabled={eliminar.isPending} className="flex-1">
                {ACCION_ELIMINAR}
              </Button>
            </DialogoEliminarRecordatorio>
          </div>
        </>
      ) : null}
    </div>
  )
}
