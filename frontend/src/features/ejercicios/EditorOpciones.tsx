import { useId, useRef } from "react"
import { Controller, useFieldArray, useWatch, type UseFormReturn } from "react-hook-form"

import { CampoTexto } from "@/components/alma/campo-texto"
import { tamanioTextoBoton } from "@/components/alma/estilos"
import { MensajeError } from "@/components/alma/mensaje-error"
import { SubirImagen } from "@/components/alma/subir-imagen"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import type { FormatoEjercicio } from "@/features/admin/etiquetas"

import {
  LIMITES_OPCIONES,
  opcionVacia,
  type DatosEjercicio,
  type EntradaEjercicio,
} from "./esquema"

type EditorOpcionesProps = {
  formulario: UseFormReturn<EntradaEjercicio, unknown, DatosEjercicio>
  formato: FormatoEjercicio
}

/**
 * Opciones del ejercicio según el formato (CU-10, contrato sección 3.2).
 * - Opción múltiple (Figma 52:1841): de 3 a 4 opciones; cada una con "Correcta" (radio
 *   nativo: una sola, y las flechas no se mezclan con los campos de texto) y su texto.
 * - Ordenar: de 3 a 5 pasos; cada uno con su texto y su "Posición" (campo numérico, sin
 *   depender de arrastrar).
 * En los dos, imagen opcional por opción (el Figma no la dibuja; sale de CU-10) y botones
 * para agregar y quitar respetando el mínimo y el máximo. Al quitar, el foco va a "Agregar".
 */
export function EditorOpciones({ formulario, formato }: EditorOpcionesProps) {
  const {
    control,
    register,
    setValue,
    formState: { errors, isSubmitted },
  } = formulario
  const { fields, append, remove } = useFieldArray({ control, name: "opciones" })
  const opciones = useWatch({ control, name: "opciones" })
  const id = useId()
  const idAyuda = `${id}-ayuda`
  const idError = `${id}-error`
  const botonAgregar = useRef<HTMLButtonElement>(null)

  const ordenar = formato === "ORDENAR"
  const { min, max } = LIMITES_OPCIONES[formato]
  const nombre = ordenar ? "paso" : "opción"
  const Nombre = ordenar ? "Paso" : "Opción"
  const errorGrupo = errors.opciones?.root?.message ?? errors.opciones?.message

  function marcarCorrecta(indice: number) {
    fields.forEach((_, i) =>
      setValue(`opciones.${i}.esCorrecta`, i === indice, {
        shouldDirty: true,
        shouldValidate: isSubmitted,
      })
    )
  }

  function quitar(indice: number) {
    remove(indice)
    // Después de que React saque la opción, el foco queda en un lugar conocido
    requestAnimationFrame(() => botonAgregar.current?.focus())
  }

  return (
    <fieldset
      aria-describedby={[idAyuda, errorGrupo ? idError : undefined].filter(Boolean).join(" ")}
      className="min-w-0"
    >
      <legend className="text-label font-semibold">
        {ordenar ? "Pasos y orden correcto" : "Opciones y respuesta correcta"}
      </legend>
      <div className="mt-2 flex flex-col gap-3">
        <p id={idAyuda} className="text-muted-foreground">
          {ordenar
            ? "Cargá de 3 a 5 pasos. En «Posición» poné el lugar de cada paso en el orden correcto (1 es el primero)."
            : "Cargá de 3 a 4 opciones y marcá cuál es la correcta."}
        </p>
        {errorGrupo ? <MensajeError id={idError}>{errorGrupo}</MensajeError> : null}

        <ol className="flex flex-col gap-3">
          {fields.map((campo, i) => {
            const n = i + 1
            const idTexto = `${id}-texto-${i}`
            const idErrorTexto = `${idTexto}-error`
            const errorTexto = errors.opciones?.[i]?.texto?.message
            const textoOpcion = opciones?.[i]?.texto?.trim() ?? ""
            return (
              <li
                key={campo.id}
                className="flex flex-col gap-3 rounded-lg border-2 border-border p-4"
              >
                <div className="flex flex-col gap-2">
                  <Label htmlFor={idTexto}>{`${Nombre} ${n}`}</Label>
                  <div className="flex flex-wrap items-center gap-2">
                    {!ordenar ? (
                      <label className="inline-flex min-h-(--altura-boton) cursor-pointer items-center gap-2 rounded-md border-2 border-transparent bg-secondary px-3 font-semibold has-checked:border-success has-checked:bg-success-soft contraste-alto:border-foreground">
                        <input
                          type="radio"
                          name={`${id}-correcta`}
                          checked={opciones?.[i]?.esCorrecta ?? false}
                          onChange={() => marcarCorrecta(i)}
                          aria-label={`Correcta: opción ${n}`}
                          className="size-5 shrink-0 cursor-pointer accent-accent"
                        />
                        Correcta
                      </label>
                    ) : null}
                    <Input
                      id={idTexto}
                      placeholder={`${Nombre} ${n}`}
                      aria-invalid={errorTexto ? true : undefined}
                      aria-describedby={errorTexto ? idErrorTexto : undefined}
                      className="min-w-[min(100%,calc(200rem/17))] flex-1"
                      {...register(`opciones.${i}.texto`)}
                    />
                  </div>
                  {errorTexto ? <MensajeError id={idErrorTexto}>{errorTexto}</MensajeError> : null}
                </div>

                {ordenar ? (
                  <CampoTexto
                    label={`Posición del paso ${n}`}
                    type="number"
                    inputMode="numeric"
                    min={1}
                    max={fields.length}
                    className="max-w-[calc(220rem/17)]"
                    {...register(`opciones.${i}.posicion`)}
                  />
                ) : null}

                <Controller
                  control={control}
                  name={`opciones.${i}.imagen`}
                  render={({ field }) => (
                    <SubirImagen
                      etiqueta={`Imagen ${ordenar ? "del paso" : "de la opción"} ${n}`}
                      textoBoton="Agregar imagen"
                      valor={field.value}
                      alCambiar={field.onChange}
                      textoAlternativo={textoOpcion || `${Nombre} ${n}`}
                      error={errors.opciones?.[i]?.imagen?.message}
                    />
                  )}
                />

                {fields.length > min ? (
                  <Button
                    variant="outline"
                    className={`self-start ${tamanioTextoBoton}`}
                    onClick={() => quitar(i)}
                  >
                    {`Quitar ${nombre} ${n}`}
                  </Button>
                ) : null}
              </li>
            )
          })}
        </ol>

        <Button
          ref={botonAgregar}
          variant="secondary"
          className={`self-start ${tamanioTextoBoton}`}
          disabled={fields.length >= max}
          onClick={() => append(opcionVacia(fields.length + 1))}
        >
          {`Agregar ${nombre}`}
        </Button>
        {fields.length >= max ? (
          <p className="text-muted-foreground">{`Ya cargaste el máximo: ${max} ${ordenar ? "pasos" : "opciones"}.`}</p>
        ) : null}
      </div>
    </fieldset>
  )
}
