import { useId, useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useNavigate } from "react-router-dom"

import { CampoTexto } from "@/components/alma/campo-texto"
import { MensajeErrorCampo } from "@/components/alma/mensaje-error-campo"
import { OpcionRadio, TildeOpcion } from "@/components/alma/opcion-radio"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { RadioGroup } from "@/components/ui/radio-group"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"
import { aplicarErroresDeCampos, mensajeDeError } from "@/lib/errores"

import { CampoDuracion } from "./CampoDuracion"
import { horaDeToma } from "./fechas"
import {
  AVISO_ACTUALIZADO,
  AVISO_CREADO,
  AVISO_MEDICACION_INVALIDA,
  EJEMPLO_DOSIS,
  EJEMPLO_NOMBRE_MEDICAMENTO,
  ETIQUETA_DOSIS,
  ETIQUETA_FRECUENCIA,
  ETIQUETA_FRECUENCIA_TITULO,
  ETIQUETA_HORA,
  ETIQUETA_NOMBRE_MEDICAMENTO,
  GUARDANDO,
  GUARDAR,
} from "./mensajes"
import {
  esquemaMedicacion,
  FRECUENCIAS,
  type DatosFormularioMedicacion,
} from "./schemas"
import type { Medicacion } from "./tipos"
import { useGuardarMedicacion } from "./useRecordatorios"

// Todos los datos de la medicación que se muestran en el formulario. `fechaInicio` no se pide
// (CU-07): al crear es hoy y al editar se conserva la que ya tenía.
const CAMPOS = ["nombreMedicamento", "dosis", "horaToma", "frecuencia", "duracionDias"] as const

/** Formulario de medicación (CU-07). Con `medicacion` edita; sin ella, crea. */
export function FormularioMedicacion({ medicacion }: { medicacion?: Medicacion }) {
  const navigate = useNavigate()
  const { avisar } = useAviso()
  const guardar = useGuardarMedicacion(medicacion?.id)
  const [aviso, setAviso] = useState<string | null>(null)
  const idFrecuencia = useId()

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<DatosFormularioMedicacion>({
    resolver: zodResolver(esquemaMedicacion),
    defaultValues: {
      nombreMedicamento: medicacion?.nombreMedicamento ?? "",
      dosis: medicacion?.dosis ?? "",
      horaToma: medicacion ? horaDeToma(medicacion.horaToma) : "",
      frecuencia: medicacion?.frecuencia ?? "DIARIA",
      duracionDias: medicacion?.duracionDias ?? 31,
    },
  })

  function enviar(datos: DatosFormularioMedicacion) {
    setAviso(null)
    guardar.mutate(datos, {
      onSuccess: () => {
        avisar(medicacion ? AVISO_ACTUALIZADO : AVISO_CREADO)
        navigate("/recordatorios")
      },
      onError: (error) => {
        const sinCampo = aplicarErroresDeCampos(error, setError, CAMPOS)
        if (error instanceof ApiError && error.status === 400) {
          setAviso([AVISO_MEDICACION_INVALIDA, ...sinCampo].join(" "))
        } else {
          setAviso(mensajeDeError(error))
        }
      },
    })
  }

  return (
    <form onSubmit={handleSubmit(enviar)} noValidate className="flex flex-col gap-5">
      {aviso ? <Alert variant="error">{aviso}</Alert> : null}

      <CampoTexto
        label={ETIQUETA_NOMBRE_MEDICAMENTO}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_NOMBRE_MEDICAMENTO}
        error={errors.nombreMedicamento?.message}
        {...register("nombreMedicamento")}
      />
      <CampoTexto
        label={ETIQUETA_DOSIS}
        type="text"
        autoComplete="off"
        placeholder={EJEMPLO_DOSIS}
        error={errors.dosis?.message}
        {...register("dosis")}
      />
      <CampoTexto
        label={ETIQUETA_HORA}
        type="time"
        error={errors.horaToma?.message}
        {...register("horaToma")}
      />

      <div className="flex flex-col gap-2">
        <p id={idFrecuencia} className="text-label font-semibold">
          {ETIQUETA_FRECUENCIA_TITULO}
        </p>
        <Controller
          control={control}
          name="frecuencia"
          render={({ field }) => (
            <RadioGroup
              aria-labelledby={idFrecuencia}
              aria-describedby={errors.frecuencia ? `${idFrecuencia}-error` : undefined}
              value={field.value}
              onValueChange={(valor) => field.onChange(valor)}
              onBlur={field.onBlur}
            >
              {FRECUENCIAS.map((frecuencia) => (
                <OpcionRadio
                  key={frecuencia}
                  value={frecuencia}
                  className="min-h-[calc(56rem/17)] flex-row items-center gap-3 px-4 py-3"
                >
                  <span className="flex-1 font-semibold">{ETIQUETA_FRECUENCIA[frecuencia]}</span>
                  <TildeOpcion className="size-6" />
                </OpcionRadio>
              ))}
            </RadioGroup>
          )}
        />
        {errors.frecuencia ? (
          <MensajeErrorCampo id={`${idFrecuencia}-error`}>{errors.frecuencia.message}</MensajeErrorCampo>
        ) : null}
      </div>

      <Controller
        control={control}
        name="duracionDias"
        render={({ field }) => (
          <CampoDuracion
            valor={field.value}
            onChange={field.onChange}
            onBlur={field.onBlur}
            error={errors.duracionDias?.message}
          />
        )}
      />

      <Button type="submit" disabled={guardar.isPending} className="w-full">
        {guardar.isPending ? GUARDANDO : GUARDAR}
      </Button>
    </form>
  )
}
