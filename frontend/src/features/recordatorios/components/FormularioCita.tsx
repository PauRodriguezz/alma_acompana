import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { format, parseISO } from "date-fns"
import { useNavigate } from "react-router-dom"

import { hoyArgentina } from "@/lib/fechas"
import { CampoArea } from "@/components/alma/campo-area"
import { CampoTexto } from "@/components/alma/campo-texto"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"
import { aplicarErroresDeCampos, mensajeDeError } from "@/lib/errores"

import {
  AVISO_ACTUALIZADO,
  AVISO_CITA_INVALIDA,
  AVISO_CREADO,
  EJEMPLO_ESPECIALIDAD,
  EJEMPLO_LUGAR,
  EJEMPLO_MEDICO,
  EJEMPLO_TITULO,
  ETIQUETA_ESPECIALIDAD,
  ETIQUETA_FECHA,
  ETIQUETA_HORA,
  ETIQUETA_INDICACIONES,
  ETIQUETA_LUGAR,
  ETIQUETA_MEDICO,
  ETIQUETA_TITULO,
  GUARDANDO,
  GUARDAR,
} from "../mensajes"
import { esquemaCita, type DatosFormularioCita } from "../schemas"
import type { Cita } from "../tipos"
import { useGuardarCita } from "../useRecordatorios"

const CAMPOS = ["titulo", "especialidad", "medico", "lugar", "fecha", "hora", "indicacionesPrevias"] as const
// La API junta fecha y hora en un solo campo: su error se muestra en el campo "Fecha".
const ALIAS = { fechaHora: "fecha" }

/** Formulario de cita médica (CU-08). Con `cita` edita; sin ella, crea. */
export function FormularioCita({ cita }: { cita?: Cita }) {
  const navigate = useNavigate()
  const { avisar } = useAviso()
  const guardar = useGuardarCita(cita?.id)
  const [aviso, setAviso] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<DatosFormularioCita>({
    resolver: zodResolver(esquemaCita),
    defaultValues: {
      titulo: cita?.titulo ?? "",
      especialidad: cita?.especialidad ?? "",
      medico: cita?.medico ?? "",
      lugar: cita?.lugar ?? "",
      // parseISO + format: la fecha llega sin zona y no hay que pasarla por new Date("...")
      fecha: cita ? format(parseISO(cita.fechaHora), "yyyy-MM-dd") : "",
      hora: cita ? format(parseISO(cita.fechaHora), "HH:mm") : "",
      indicacionesPrevias: cita?.indicacionesPrevias ?? "",
    },
  })

  function enviar(datos: DatosFormularioCita) {
    setAviso(null)
    guardar.mutate(
      {
        titulo: datos.titulo,
        especialidad: datos.especialidad,
        medico: datos.medico,
        lugar: datos.lugar,
        fechaHora: `${datos.fecha}T${datos.hora}`,
        indicacionesPrevias: datos.indicacionesPrevias || undefined,
      },
      {
        onSuccess: () => {
          avisar(cita ? AVISO_ACTUALIZADO : AVISO_CREADO)
          navigate("/recordatorios")
        },
        onError: (error) => {
          const sinCampo = aplicarErroresDeCampos(error, setError, CAMPOS, ALIAS)
          if (error instanceof ApiError && error.status === 400) {
            setAviso([AVISO_CITA_INVALIDA, ...sinCampo].join(" "))
          } else {
            setAviso(mensajeDeError(error))
          }
        },
      }
    )
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate className="flex flex-col gap-5">
      {aviso ? <Alert variant="error">{aviso}</Alert> : null}

      <CampoTexto
        label={ETIQUETA_TITULO}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_TITULO}
        error={errors.titulo?.message}
        {...register("titulo")}
      />
      <CampoTexto
        label={ETIQUETA_ESPECIALIDAD}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_ESPECIALIDAD}
        error={errors.especialidad?.message}
        {...register("especialidad")}
      />
      <CampoTexto
        label={ETIQUETA_MEDICO}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_MEDICO}
        error={errors.medico?.message}
        {...register("medico")}
      />
      <CampoTexto
        label={ETIQUETA_LUGAR}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_LUGAR}
        error={errors.lugar?.message}
        {...register("lugar")}
      />
      <CampoTexto
        label={ETIQUETA_FECHA}
        type="date"
        min={hoyArgentina()}
        error={errors.fecha?.message}
        {...register("fecha")}
      />
      <CampoTexto
        label={ETIQUETA_HORA}
        type="time"
        error={errors.hora?.message}
        {...register("hora")}
      />
      <CampoArea
        label={ETIQUETA_INDICACIONES}
        error={errors.indicacionesPrevias?.message}
        {...register("indicacionesPrevias")}
      />

      <Button type="submit" disabled={guardar.isPending} className="w-full">
        {guardar.isPending ? GUARDANDO : GUARDAR}
      </Button>
    </form>
  )
}
