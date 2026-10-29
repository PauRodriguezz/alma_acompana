import { useEffect, useRef, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { Controller, useForm, useWatch, type DefaultValues } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { CampoAreaTexto } from "@/components/alma/campo-area-texto"
import { CampoTexto } from "@/components/alma/campo-texto"
import { SelectorChips } from "@/components/alma/selector-chips"
import { SubirImagen } from "@/components/alma/subir-imagen"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { EncabezadoPagina } from "@/features/admin/EncabezadoPagina"
import { mensajeDeError } from "@/lib/errores"
import { hoyArgentina } from "@/lib/fechas"
import { CATEGORIAS_MICROCAPSULA, TEXTO_CATEGORIA_MICROCAPSULA } from "@/features/admin/etiquetas"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"

import {
  CLAVES_MICROCAPSULAS,
  crearMicrocapsula,
  editarMicrocapsula,
  obtenerMicrocapsula,
  refrescarMicrocapsulas,
} from "./api"
import {
  esquemaMicrocapsula,
  TEXTO_CAMPOS_REQUERIDOS,
  TEXTO_REVISAR,
  type DatosMicrocapsula,
  type EntradaMicrocapsula,
} from "./schemas"
import { VistaPreviaMicrocapsula } from "./VistaPreviaMicrocapsula"
import {
  ACCION_CANCELAR,
  ACCION_GUARDANDO,
  ESTADO_CARGANDO,
  ETIQUETA_CATEGORIA,
  MENSAJE_NO_ENCONTRADO,
} from "@/lib/mensajes"
import { ADMIN_MICROCAPSULAS, ADMIN_PANEL } from "@/features/admin/mensajes"
import {
  AVISO_ACTUALIZADA,
  AVISO_PUBLICADA,
  AVISO_SE_PUBLICA,
  BOTON_GUARDAR,
  EJEMPLO_TEXTO,
  EJEMPLO_TITULO,
  ETIQUETA_FECHA_PUBLICACION,
  ETIQUETA_IMAGEN,
  ETIQUETA_TEXTO,
  ETIQUETA_TITULO,
  IMAGEN_ALTERNATIVO,
  MIGA_EDITAR,
  MIGA_NUEVA,
  NUEVA_MICROCAPSULA,
  TITULO_EDITAR,
  VOLVER_A_MICROCAPSULAS,
} from "./mensajes"

const RUTA_LISTA = "/admin/microcapsulas"
const CAMPOS: (keyof EntradaMicrocapsula)[] = [
  "titulo",
  "texto",
  "categoria",
  "imagen",
  "fechaPublicacion",
]
const OPCIONES_CATEGORIA = CATEGORIAS_MICROCAPSULA.map((valor) => ({
  valor,
  texto: TEXTO_CATEGORIA_MICROCAPSULA[valor],
}))

type Franja = { texto: string; clave: number }

type FormularioMicrocapsulaProps = {
  /** undefined al crear. */
  id?: number
  /** Al crear, la categoría arranca sin elegir. */
  inicial: DefaultValues<EntradaMicrocapsula>
}

/**
 * Formulario de microcápsula (Figma 52:1894 y 52:2140, CU-11). La franja roja de arriba
 * muestra el detail (del back o el mismo texto, calculado antes de enviar) y recibe el
 * foco; cada campo con error lleva su mensaje con ícono. Con el contenido ancho, la vista
 * previa va a la derecha; si no, debajo del formulario.
 */
function FormularioMicrocapsula({ id, inicial }: FormularioMicrocapsulaProps) {
  const navigate = useNavigate()
  const clienteConsultas = useQueryClient()
  const { avisar } = useAviso()
  const refFranja = useRef<HTMLDivElement>(null)
  const [franja, setFranja] = useState<Franja | null>(null)

  const {
    register,
    control,
    handleSubmit,
    getValues,
    setError,
    formState: { errors },
  } = useForm<EntradaMicrocapsula, unknown, DatosMicrocapsula>({
    resolver: zodResolver(esquemaMicrocapsula),
    defaultValues: inicial,
    // El foco va a la franja roja, que explica qué pasó; desde ahí se sigue con Tab.
    shouldFocusError: false,
  })

  const [titulo, categoria, imagen] = useWatch({
    control,
    name: ["titulo", "categoria", "imagen"],
  })

  useEffect(() => {
    if (franja) refFranja.current?.focus()
  }, [franja])

  // La clave cambia en cada intento, así el mismo texto vuelve a enfocarse y anunciarse.
  const mostrarFranja = (texto: string) =>
    setFranja((anterior) => ({ texto, clave: (anterior?.clave ?? 0) + 1 }))

  const guardar = useMutation({
    mutationFn: (datos: DatosMicrocapsula) =>
      id === undefined ? crearMicrocapsula(datos) : editarMicrocapsula(id, datos),
    onSuccess: async () => {
      await refrescarMicrocapsulas(clienteConsultas)
      avisar(
        id === undefined ? AVISO_PUBLICADA : AVISO_ACTUALIZADA
      )
      navigate(RUTA_LISTA)
    },
    onError: (e) => {
      mostrarFranja(mensajeDeError(e))
      if (e instanceof ApiError) {
        for (const { campo, mensaje } of e.errores ?? []) {
          if ((CAMPOS as string[]).includes(campo)) {
            setError(campo as keyof EntradaMicrocapsula, { type: "server", message: mensaje })
          }
        }
      }
    },
  })

  function alFallarValidacion() {
    const v = getValues()
    const faltaAlgo =
      !v.titulo.trim() || !v.texto.trim() || !v.categoria || !v.fechaPublicacion
    mostrarFranja(faltaAlgo ? TEXTO_CAMPOS_REQUERIDOS : TEXTO_REVISAR)
  }

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
        <Card className="@container gap-5">
          <CampoTexto
            label={ETIQUETA_TITULO}
            placeholder={EJEMPLO_TITULO}
            error={errors.titulo?.message}
            {...register("titulo")}
          />
          <CampoAreaTexto
            label={ETIQUETA_TEXTO}
            placeholder={EJEMPLO_TEXTO}
            rows={6}
            error={errors.texto?.message}
            {...register("texto")}
          />
          <Controller
            control={control}
            name="categoria"
            render={({ field }) => (
              <SelectorChips
                etiqueta={ETIQUETA_CATEGORIA}
                opciones={OPCIONES_CATEGORIA}
                valor={field.value}
                alCambiar={field.onChange}
                error={errors.categoria?.message}
              />
            )}
          />
          <div className="grid items-start gap-5 @lg:grid-cols-2">
            <Controller
              control={control}
              name="imagen"
              render={({ field }) => (
                <SubirImagen
                  etiqueta={ETIQUETA_IMAGEN}
                  valor={field.value}
                  alCambiar={field.onChange}
                  textoAlternativo={titulo.trim() || IMAGEN_ALTERNATIVO}
                  error={errors.imagen?.message}
                />
              )}
            />
            <CampoTexto
              label={ETIQUETA_FECHA_PUBLICACION}
              type="date"
              max={hoyArgentina()}
              error={errors.fechaPublicacion?.message}
              {...register("fechaPublicacion")}
            />
          </div>
          <Alert variant="info" role="note">
            {AVISO_SE_PUBLICA}
          </Alert>
        </Card>

        <VistaPreviaMicrocapsula titulo={titulo} categoria={categoria} imagen={imagen} />
      </div>

      <div className="flex flex-wrap justify-end gap-3">
        <Link to={RUTA_LISTA} className={buttonVariants({ variant: "outline" })}>
          {ACCION_CANCELAR}
        </Link>
        <Button type="submit" disabled={guardar.isPending}>
          {guardar.isPending ? ACCION_GUARDANDO : BOTON_GUARDAR}
        </Button>
      </div>
    </form>
  )
}

/** Nueva microcápsula (/admin/microcapsulas/nueva) y editar (/admin/microcapsulas/:id/editar). */
export function MicrocapsulaFormPage() {
  const { id: idTexto } = useParams()
  const editando = idTexto !== undefined
  const id = editando ? Number(idTexto) : undefined
  const idValido = id === undefined || (Number.isInteger(id) && id > 0)

  const detalle = useQuery({
    queryKey: CLAVES_MICROCAPSULAS.detalle(id ?? 0),
    queryFn: () => obtenerMicrocapsula(id as number),
    enabled: editando && idValido,
  })

  const titulo = editando ? TITULO_EDITAR : NUEVA_MICROCAPSULA
  const encabezado = (
    <EncabezadoPagina
      titulo={titulo}
      migas={[
        { texto: ADMIN_PANEL, a: "/admin" },
        { texto: ADMIN_MICROCAPSULAS, a: RUTA_LISTA },
        { texto: editando ? MIGA_EDITAR : MIGA_NUEVA },
      ]}
    />
  )

  if (!editando) {
    return (
      <>
        {encabezado}
        <FormularioMicrocapsula
          inicial={{
            titulo: "",
            texto: "",
            categoria: undefined,
            imagen: null,
            fechaPublicacion: hoyArgentina(),
          }}
        />
      </>
    )
  }

  if (!idValido || detalle.isError) {
    return (
      <>
        {encabezado}
        <Alert variant="error">
          <div className="flex flex-col items-start gap-3">
            <p>{idValido ? mensajeDeError(detalle.error) : MENSAJE_NO_ENCONTRADO}</p>
            <Link to={RUTA_LISTA} className={buttonVariants({ variant: "outline" })}>
              {VOLVER_A_MICROCAPSULAS}
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
        <p role="status">{ESTADO_CARGANDO}</p>
      </>
    )
  }

  const m = detalle.data
  return (
    <>
      {encabezado}
      <FormularioMicrocapsula
        key={m.id}
        id={m.id}
        inicial={{
          titulo: m.titulo,
          texto: m.texto,
          categoria: m.categoria,
          imagen: m.imagen,
          fechaPublicacion: m.fechaPublicacion,
        }}
      />
    </>
  )
}
