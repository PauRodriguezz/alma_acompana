import { useEffect, useId, useRef, useState, type RefObject } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { Controller, useForm, useWatch, type DefaultValues } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import {
  Heading01Icon,
  Heading02Icon,
  LeftToRightListBulletIcon,
  LeftToRightListNumberIcon,
  QuoteDownIcon,
  TextBoldIcon,
  TextItalicIcon,
} from "@hugeicons/core-free-icons"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { CampoAreaTexto } from "@/components/formularios/CampoAreaTexto"
import { CampoTexto } from "@/components/formularios/CampoTexto"
import { SelectorChips } from "@/components/formularios/SelectorChips"
import { SubirImagen } from "@/components/formularios/SubirImagen"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { PaginaDeFormulario } from "@/features/admin/EncabezadoPagina"
import { mensajeDeError } from "@/lib/errores"
import { hoyArgentina } from "@/lib/fechas"
import { CATEGORIAS_MICROCAPSULA, TEXTO_CATEGORIA_MICROCAPSULA } from "@/features/admin/etiquetas"
import { useAviso } from "@/features/avisos/useAviso"
import { ApiError } from "@/lib/api"
import { cn } from "@/lib/utils"

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
  ETIQUETA_CATEGORIA,
} from "@/lib/mensajes"
import { ADMIN_MICROCAPSULAS, ADMIN_PANEL } from "@/features/admin/mensajes"
import {
  AYUDA_FORMATO,
  FORMATO_CURSIVA,
  FORMATO_DESTACADO,
  FORMATO_ETIQUETA,
  FORMATO_LISTA,
  FORMATO_LISTA_NUMERADA,
  FORMATO_NEGRITA,
  FORMATO_SUBTITULO,
  FORMATO_TEXTO_EJEMPLO,
  FORMATO_TITULO,
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

// Barra de formato del texto (contrato de microcápsulas, sección 4): escribe la marca de Markdown
// en el área de texto (##, ###, **, *, -, 1., >). El área es de react-hook-form y no se vuelve a
// montar: se cambia su valor desde acá (alCambiar) y se deja la selección donde estaba.
// Sin tamaños ni colores: solo la gramática que muestra TextoLectura.

type Seleccion = { texto: string; desde: number; hasta: number }

const PREFIJO_DE_BLOQUE = /^(#{2,3} |[-*] |\d+\. |> )/

type Bloque = { prefijo: (n: number) => string; reconoce: RegExp }

const TITULO: Bloque = { prefijo: () => "## ", reconoce: /^## / }
const SUBTITULO: Bloque = { prefijo: () => "### ", reconoce: /^### / }
const LISTA: Bloque = { prefijo: () => "- ", reconoce: /^[-*] / }
const NUMERADA: Bloque = { prefijo: (n) => `${n}. `, reconoce: /^\d+\. / }
const DESTACADO: Bloque = { prefijo: () => "> ", reconoce: /^> / }

/** Envuelve la selección (o un texto de ejemplo, que queda seleccionado) con la marca. */
function envolver(area: HTMLTextAreaElement, marca: string): Seleccion {
  const { value, selectionStart: i, selectionEnd: f } = area
  const elegido = value.slice(i, f) || FORMATO_TEXTO_EJEMPLO
  const texto = value.slice(0, i) + marca + elegido + marca + value.slice(f)
  return { texto, desde: i + marca.length, hasta: i + marca.length + elegido.length }
}

/** Pone (o saca, si todas las líneas ya lo tienen) el prefijo del bloque en cada línea elegida. */
function aplicarBloque(area: HTMLTextAreaElement, bloque: Bloque): Seleccion {
  const { value, selectionStart: i, selectionEnd: f } = area
  const inicio = value.lastIndexOf("\n", i - 1) + 1
  let fin = value.indexOf("\n", f > i && value[f - 1] === "\n" ? f - 1 : f)
  if (fin === -1) fin = value.length

  const lineas = value.slice(inicio, fin).split("\n")
  const conTexto = lineas.filter((linea) => linea.trim() !== "")
  const yaLoTienen = conTexto.length > 0 && conTexto.every((linea) => bloque.reconoce.test(linea))

  let n = 0
  const nuevas = lineas.map((linea) => {
    if (linea.trim() === "") return linea
    const limpia = linea.replace(PREFIJO_DE_BLOQUE, "")
    if (yaLoTienen) return limpia
    n += 1
    return bloque.prefijo(n) + limpia
  })
  const bloqueNuevo = nuevas.join("\n")
  return {
    texto: value.slice(0, inicio) + bloqueNuevo + value.slice(fin),
    desde: inicio,
    hasta: inicio + bloqueNuevo.length,
  }
}

type BarraFormatoProps = {
  areaRef: RefObject<HTMLTextAreaElement | null>
  /** id del área de texto que formatea (aria-controls). */
  controla: string
  /** Recibe el texto completo ya con el formato aplicado. */
  alCambiar: (texto: string) => void
}

/**
 * Barra de solo íconos pegada arriba del área de texto: forma con ella un solo bloque (barra de
 * borde redondeado arriba y campo abajo, como un editor de texto enriquecido).
 * Es una excepción a "un ícono nunca va solo" (contrato del panel, sección 6): cada botón tiene
 * aria-label con el nombre del formato y una etiqueta que aparece al pasar el mouse y al enfocar con
 * el teclado (aria-hidden: el lector de pantalla ya lee el aria-label). Se opera con Tab y Enter o
 * Espacio. Cada botón mide 44 px como mínimo y escala con la letra.
 */
export function BarraFormato({ areaRef, controla, alCambiar }: BarraFormatoProps) {
  function aplicar(cambio: (area: HTMLTextAreaElement) => Seleccion) {
    const area = areaRef.current
    if (!area) return
    const { texto, desde, hasta } = cambio(area)
    alCambiar(texto)
    // El valor ya cambió; se devuelve el foco al área con la selección en el texto tocado.
    requestAnimationFrame(() => {
      area.focus()
      area.setSelectionRange(desde, hasta)
    })
  }

  return (
    <div
      role="toolbar"
      aria-label={FORMATO_ETIQUETA}
      aria-controls={controla}
      aria-orientation="horizontal"
      className="flex flex-wrap items-center gap-y-1 rounded-t-md border-2 border-b-0 border-input bg-secondary p-1 contraste-alto:border-foreground contraste-alto:bg-card"
    >
      {GRUPOS.map((grupo, i) => (
        <div
          key={i}
          className={cn(
            "flex items-center gap-1",
            i > 0 && "ml-1 border-l-2 border-border pl-1 contraste-alto:border-foreground"
          )}
        >
          {grupo.map(({ etiqueta, icono, cambio }) => (
            <button
              key={etiqueta}
              type="button"
              aria-label={etiqueta}
              onClick={() => aplicar(cambio)}
              className="group relative inline-flex min-h-11 min-w-11 items-center justify-center rounded-md border-2 border-transparent text-heading hover:bg-outline-hover contraste-alto:hover:border-foreground"
            >
              <HugeiconsIcon icon={icono} className="size-6" aria-hidden="true" />
              <span
                aria-hidden="true"
                className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 rounded-md bg-foreground px-2 py-1 text-caption font-semibold whitespace-nowrap text-background group-hover:block group-focus-visible:block contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card contraste-alto:text-foreground"
              >
                {etiqueta}
              </span>
            </button>
          ))}
        </div>
      ))}
    </div>
  )
}

type BotonFormato = {
  etiqueta: string
  icono: IconSvgElement
  cambio: (area: HTMLTextAreaElement) => Seleccion
}

// Títulos | estilo | listas | destacado
const GRUPOS: BotonFormato[][] = [
  [
    { etiqueta: FORMATO_TITULO, icono: Heading01Icon, cambio: (a) => aplicarBloque(a, TITULO) },
    {
      etiqueta: FORMATO_SUBTITULO,
      icono: Heading02Icon,
      cambio: (a) => aplicarBloque(a, SUBTITULO),
    },
  ],
  [
    { etiqueta: FORMATO_NEGRITA, icono: TextBoldIcon, cambio: (a) => envolver(a, "**") },
    { etiqueta: FORMATO_CURSIVA, icono: TextItalicIcon, cambio: (a) => envolver(a, "*") },
  ],
  [
    { etiqueta: FORMATO_LISTA, icono: LeftToRightListBulletIcon, cambio: (a) => aplicarBloque(a, LISTA) },
    {
      etiqueta: FORMATO_LISTA_NUMERADA,
      icono: LeftToRightListNumberIcon,
      cambio: (a) => aplicarBloque(a, NUMERADA),
    },
  ],
  [
    {
      etiqueta: FORMATO_DESTACADO,
      icono: QuoteDownIcon,
      cambio: (a) => aplicarBloque(a, DESTACADO),
    },
  ],
]

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
    setValue,
    formState: { errors, isSubmitted },
  } = useForm<EntradaMicrocapsula, unknown, DatosMicrocapsula>({
    resolver: zodResolver(esquemaMicrocapsula),
    defaultValues: inicial,
    // El foco va a la franja roja, que explica qué pasó; desde ahí se sigue con Tab.
    shouldFocusError: false,
  })

  const [titulo, texto, categoria, imagen] = useWatch({
    control,
    name: ["titulo", "texto", "categoria", "imagen"],
  })

  // El área de texto la maneja react-hook-form; la barra de formato necesita el elemento para leer
  // la selección, así que se guarda también acá.
  const areaTexto = useRef<HTMLTextAreaElement | null>(null)
  const idTexto = useId()
  const { ref: refTextoForm, ...registroTexto } = register("texto")
  const aplicarFormato = (nuevo: string) =>
    setValue("texto", nuevo, { shouldDirty: true, shouldValidate: isSubmitted })

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
            id={idTexto}
            label={ETIQUETA_TEXTO}
            ayuda={AYUDA_FORMATO}
            barra={<BarraFormato areaRef={areaTexto} controla={idTexto} alCambiar={aplicarFormato} />}
            placeholder={EJEMPLO_TEXTO}
            rows={10}
            error={errors.texto?.message}
            {...registroTexto}
            ref={(el) => {
              refTextoForm(el)
              areaTexto.current = el
            }}
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

        <VistaPreviaMicrocapsula titulo={titulo} texto={texto} categoria={categoria} imagen={imagen} />
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

  return (
    <PaginaDeFormulario
      titulo={editando ? TITULO_EDITAR : NUEVA_MICROCAPSULA}
      migas={[
        { texto: ADMIN_PANEL, a: "/admin" },
        { texto: ADMIN_MICROCAPSULAS, a: RUTA_LISTA },
        { texto: editando ? MIGA_EDITAR : MIGA_NUEVA },
      ]}
      editando={editando}
      idValido={idValido}
      detalle={detalle}
      rutaLista={RUTA_LISTA}
      textoVolver={VOLVER_A_MICROCAPSULAS}
      formularioNuevo={
        <FormularioMicrocapsula
          inicial={{
            titulo: "",
            texto: "",
            categoria: undefined,
            imagen: null,
            fechaPublicacion: hoyArgentina(),
          }}
        />
      }
      formularioEditar={(m) => (
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
      )}
    />
  )
}
