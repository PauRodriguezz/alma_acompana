import { useEffect, useRef, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import {
  Controller,
  useForm,
  useWatch,
  type DefaultValues,
  type FieldErrors,
  type Path,
} from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { CampoAreaTexto } from "@/components/alma/campo-area-texto"
import { tamanioTextoBoton } from "@/components/alma/estilos"
import { SelectorChips } from "@/components/alma/selector-chips"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { conAviso } from "@/features/admin/aviso"
import { EncabezadoPagina } from "@/features/admin/EncabezadoPagina"
import { mensajeDeError } from "@/features/admin/errores"
import {
  CATEGORIAS_EJERCICIO,
  FORMATOS_EJERCICIO,
  NIVELES_DIFICULTAD,
  TEXTO_CATEGORIA_EJERCICIO,
  TEXTO_FORMATO,
  TEXTO_NIVEL,
  type FormatoEjercicio,
} from "@/features/admin/etiquetas"
import { ApiError } from "@/lib/api"

import {
  CLAVES_EJERCICIOS,
  crearEjercicio,
  editarEjercicio,
  obtenerEjercicio,
  refrescarEjercicios,
} from "./api"
import { EditorOpciones } from "./EditorOpciones"
import {
  aCuerpo,
  esquemaEjercicio,
  opcionVacia,
  TEXTO_CAMPOS_REQUERIDOS,
  TEXTO_REVISAR,
  type DatosEjercicio,
  type EntradaEjercicio,
} from "./esquema"
import type { EjercicioDetalle } from "./tipos"
import { VistaPreviaEjercicio } from "./VistaPreviaEjercicio"

const RUTA_LISTA = "/admin/ejercicios"
const AYUDA_EXPLICACION =
  "Se muestra después de responder. Ejemplo: Este ejercicio trata sobre completar con la palabra correcta el refrán que te aparece."

const OPCIONES_CATEGORIA = CATEGORIAS_EJERCICIO.map((valor) => ({
  valor,
  texto: TEXTO_CATEGORIA_EJERCICIO[valor],
}))
const OPCIONES_FORMATO = FORMATOS_EJERCICIO.map((valor) => ({ valor, texto: TEXTO_FORMATO[valor] }))
const OPCIONES_NIVEL = NIVELES_DIFICULTAD.map((valor) => ({ valor, texto: TEXTO_NIVEL[valor] }))

const CAMPOS_SIMPLES = [
  "categoria",
  "formato",
  "consigna",
  "nivelDificultad",
  "explicacion",
  "textoAMemorizar",
]

/**
 * Campo del back (Problem Details) → campo del formulario: "opciones" va al error del
 * grupo y "opciones[2].texto" a la opción 3.
 */
function campoDelFormulario(campo: string): Path<EntradaEjercicio> | "opciones.root" | null {
  if (CAMPOS_SIMPLES.includes(campo)) return campo as Path<EntradaEjercicio>
  if (campo === "opciones") return "opciones.root"
  const opcion = /^opciones\[(\d+)\]\.(texto|imagen)$/.exec(campo)
  if (opcion) return `opciones.${Number(opcion[1])}.${opcion[2] as "texto" | "imagen"}`
  return null
}

const VALORES_NUEVO: DefaultValues<EntradaEjercicio> = {
  categoria: undefined,
  // PROPUESTO: el formato arranca en "Elegir una opción" (como el Figma), porque de él
  // depende el editor de opciones.
  formato: "OPCION_MULTIPLE",
  consigna: "",
  nivelDificultad: undefined,
  explicacion: "",
  textoAMemorizar: "",
  opciones: [opcionVacia(1), opcionVacia(2), opcionVacia(3)],
}

function valoresDe(e: EjercicioDetalle): DefaultValues<EntradaEjercicio> {
  return {
    categoria: e.categoria,
    formato: e.formato,
    consigna: e.consigna,
    nivelDificultad: e.nivelDificultad,
    explicacion: e.explicacion ?? "",
    textoAMemorizar: e.textoAMemorizar ?? "",
    opciones: e.opciones.map((o, i) => ({
      texto: o.texto,
      esCorrecta: o.esCorrecta === true,
      imagen: o.imagen,
      posicion: String(o.ordenCorrecto ?? i + 1),
    })),
  }
}

type Franja = { texto: string; clave: number }

/**
 * Formulario de ejercicio (Figma 52:1765 y 52:2005, CU-10). Igual que el de microcápsula:
 * franja roja arriba con el detail (recibe el foco) y mensaje junto a cada campo.
 */
function FormularioEjercicio({ id, inicial }: { id?: number; inicial: DefaultValues<EntradaEjercicio> }) {
  const navigate = useNavigate()
  const clienteConsultas = useQueryClient()
  const refFranja = useRef<HTMLDivElement>(null)
  const [franja, setFranja] = useState<Franja | null>(null)

  const formulario = useForm<EntradaEjercicio, unknown, DatosEjercicio>({
    resolver: zodResolver(esquemaEjercicio),
    defaultValues: inicial,
    shouldFocusError: false,
  })
  const {
    register,
    control,
    handleSubmit,
    getValues,
    setValue,
    setError,
    formState: { errors },
  } = formulario

  const [categoria, formato, consigna, textoAMemorizar, opciones] = useWatch({
    control,
    name: ["categoria", "formato", "consigna", "textoAMemorizar", "opciones"],
  })

  useEffect(() => {
    if (franja) refFranja.current?.focus()
  }, [franja])

  const mostrarFranja = (texto: string) =>
    setFranja((anterior) => ({ texto, clave: (anterior?.clave ?? 0) + 1 }))

  const guardar = useMutation({
    mutationFn: (datos: DatosEjercicio) =>
      id === undefined ? crearEjercicio(aCuerpo(datos)) : editarEjercicio(id, aCuerpo(datos)),
    onSuccess: async () => {
      await refrescarEjercicios(clienteConsultas)
      navigate(
        RUTA_LISTA,
        conAviso(id === undefined ? "El ejercicio fue creado." : "El ejercicio fue actualizado.")
      )
    },
    onError: (e) => {
      mostrarFranja(mensajeDeError(e))
      if (e instanceof ApiError) {
        for (const { campo, mensaje } of e.errores ?? []) {
          const destino = campoDelFormulario(campo)
          // "opciones.root" es el error de toda la lista (convención de react-hook-form);
          // sus tipos no lo incluyen, pero lo guarda en errors.opciones.root.
          if (destino) {
            setError(destino as Path<EntradaEjercicio>, { type: "server", message: mensaje })
          }
        }
      }
    },
  })

  // Mismo orden que el back: primero los obligatorios, después las opciones, después el resto.
  function alFallarValidacion(errores: FieldErrors<EntradaEjercicio>) {
    const v = getValues()
    const faltaAlgo =
      !v.categoria ||
      !v.formato ||
      !v.consigna.trim() ||
      !v.nivelDificultad ||
      (v.categoria === "MEMORIA" && !v.textoAMemorizar.trim()) ||
      v.opciones.some((o) => !o.texto.trim())
    // zod corre la regla del texto a memorizar solo si el resto de los campos pasó: si falta
    // junto con otros, se marca acá para que el campo también muestre su mensaje.
    if (v.categoria === "MEMORIA" && !v.textoAMemorizar.trim() && !errores.textoAMemorizar) {
      setError("textoAMemorizar", { type: "required", message: "Ingresá el texto a memorizar." })
    }
    const errorOpciones = errores.opciones?.root?.message ?? errores.opciones?.message
    mostrarFranja(faltaAlgo ? TEXTO_CAMPOS_REQUERIDOS : (errorOpciones ?? TEXTO_REVISAR))
  }

  // Al pasar a "Ordenar los pasos", los pasos sin posición reciben la que les toca según la lista.
  function cambiarFormato(nuevo: FormatoEjercicio, onChange: (valor: FormatoEjercicio) => void) {
    onChange(nuevo)
    if (nuevo === "ORDENAR") {
      getValues("opciones").forEach((o, i) => {
        if (!o.posicion.trim()) setValue(`opciones.${i}.posicion`, String(i + 1))
      })
    }
  }

  const formatoActual = formato ?? "OPCION_MULTIPLE"

  return (
    <form
      noValidate
      onSubmit={handleSubmit((datos) => guardar.mutate(datos), alFallarValidacion)}
      className="flex flex-col gap-6"
    >
      {franja ? (
        <Alert key={franja.clave} ref={refFranja} tabIndex={-1} variant="error">
          {franja.texto}
        </Alert>
      ) : null}

      <div className="grid items-start gap-6 @3xl:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Card className="gap-6">
          <Controller
            control={control}
            name="categoria"
            render={({ field }) => (
              <SelectorChips
                etiqueta="Categoría"
                opciones={OPCIONES_CATEGORIA}
                valor={field.value}
                alCambiar={field.onChange}
                error={errors.categoria?.message}
              />
            )}
          />
          <CampoAreaTexto
            label="Consigna"
            placeholder="Por ejemplo: Completá el refrán: «Más vale pájaro en mano…»"
            rows={3}
            error={errors.consigna?.message}
            {...register("consigna")}
          />
          {categoria === "MEMORIA" ? (
            <CampoAreaTexto
              label="Texto a memorizar"
              ayuda="Es lo que la persona lee antes de responder."
              rows={5}
              error={errors.textoAMemorizar?.message}
              {...register("textoAMemorizar")}
            />
          ) : null}
          <Controller
            control={control}
            name="formato"
            render={({ field }) => (
              <SelectorChips
                etiqueta="Formato del ejercicio"
                opciones={OPCIONES_FORMATO}
                valor={field.value}
                alCambiar={(nuevo) => cambiarFormato(nuevo, field.onChange)}
                error={errors.formato?.message}
              />
            )}
          />
          <EditorOpciones formulario={formulario} formato={formatoActual} />
          <Controller
            control={control}
            name="nivelDificultad"
            render={({ field }) => (
              <SelectorChips
                etiqueta="Nivel de dificultad"
                opciones={OPCIONES_NIVEL}
                valor={field.value}
                alCambiar={field.onChange}
                error={errors.nivelDificultad?.message}
              />
            )}
          />
          <CampoAreaTexto
            label="Explicación"
            opcional
            ayuda={AYUDA_EXPLICACION}
            rows={3}
            error={errors.explicacion?.message}
            {...register("explicacion")}
          />
        </Card>

        <VistaPreviaEjercicio
          categoria={categoria}
          formato={formato}
          consigna={consigna ?? ""}
          textoAMemorizar={textoAMemorizar ?? ""}
          opciones={opciones ?? []}
        />
      </div>

      <div className="flex flex-wrap justify-end gap-3">
        <Link to={RUTA_LISTA} className={buttonVariants({ variant: "outline" })}>
          Cancelar
        </Link>
        <Button type="submit" className={tamanioTextoBoton} disabled={guardar.isPending}>
          {guardar.isPending ? "Guardando…" : "Guardar ejercicio"}
        </Button>
      </div>
    </form>
  )
}

/** Nuevo ejercicio (/admin/ejercicios/nuevo) y editar (/admin/ejercicios/:id/editar). */
export function EjercicioFormPage() {
  const { id: idTexto } = useParams()
  const editando = idTexto !== undefined
  const id = editando ? Number(idTexto) : undefined
  const idValido = id === undefined || (Number.isInteger(id) && id > 0)

  const detalle = useQuery({
    queryKey: CLAVES_EJERCICIOS.detalle(id ?? 0),
    queryFn: () => obtenerEjercicio(id as number),
    enabled: editando && idValido,
  })

  const encabezado = (
    <EncabezadoPagina
      titulo={editando ? "Editar ejercicio" : "Nuevo ejercicio"}
      migas={[
        { texto: "Panel de operador", a: "/admin" },
        { texto: "Ejercicios", a: RUTA_LISTA },
        { texto: editando ? "Editar" : "Nuevo" },
      ]}
    />
  )

  if (!editando) {
    return (
      <>
        {encabezado}
        <FormularioEjercicio inicial={VALORES_NUEVO} />
      </>
    )
  }

  if (!idValido || detalle.isError) {
    return (
      <>
        {encabezado}
        <Alert variant="error">
          <div className="flex flex-col items-start gap-3">
            <p>{idValido ? mensajeDeError(detalle.error) : "No encontramos lo que buscás."}</p>
            <Link to={RUTA_LISTA} className={buttonVariants({ variant: "outline" })}>
              Volver a ejercicios
            </Link>
          </div>
        </Alert>
      </>
    )
  }

  if (detalle.isPending) {
    return (
      <>
        {encabezado}
        <p role="status">Cargando…</p>
      </>
    )
  }

  return (
    <>
      {encabezado}
      <FormularioEjercicio key={detalle.data.id} id={detalle.data.id} inicial={valoresDe(detalle.data)} />
    </>
  )
}
