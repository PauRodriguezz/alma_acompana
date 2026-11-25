import { Fragment, useEffect, type ReactNode } from "react"
import type { UseQueryResult } from "@tanstack/react-query"
import { Link } from "react-router-dom"

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Alert } from "@/components/ui/alert"
import { buttonVariants } from "@/components/ui/button-variants"
import { mensajeDeError } from "@/lib/errores"
import { ESTADO_CARGANDO, MENSAJE_NO_ENCONTRADO } from "@/lib/mensajes"

import { ADMIN_TITULO_PESTANIA } from "./mensajes"

type Miga = { texto: string; a?: string }

type EncabezadoPaginaProps = {
  titulo: string
  /** Migas de pan; la última es la página actual (sin enlace). */
  migas?: Miga[]
  /** Acción principal a la derecha del título (por ejemplo "Nuevo ejercicio"). */
  accion?: ReactNode
}

/**
 * Encabezado de las pantallas del panel (Figma 51:1409 y 51:1694): migas de pan, título
 * h1-desktop y, a la derecha, la acción principal. Si no entra (celular o letra grande),
 * la acción baja debajo del título. También pone el título de la pestaña del navegador.
 */
export function EncabezadoPagina({ titulo, migas, accion }: EncabezadoPaginaProps) {
  useEffect(() => {
    document.title = ADMIN_TITULO_PESTANIA(titulo)
  }, [titulo])

  return (
    <div className="flex flex-col gap-2">
      {migas && migas.length > 0 ? (
        <Breadcrumb>
          <BreadcrumbList>
            {migas.map((miga, i) => (
              <Fragment key={miga.texto}>
                {i > 0 ? <BreadcrumbSeparator /> : null}
                <BreadcrumbItem>
                  {miga.a ? (
                    <BreadcrumbLink render={<Link to={miga.a} />}>{miga.texto}</BreadcrumbLink>
                  ) : (
                    <BreadcrumbPage>{miga.texto}</BreadcrumbPage>
                  )}
                </BreadcrumbItem>
              </Fragment>
            ))}
          </BreadcrumbList>
        </Breadcrumb>
      ) : null}
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2">
        <h1 className="min-w-0">{titulo}</h1>
        {accion}
      </div>
    </div>
  )
}

type PaginaDeFormularioProps<T> = {
  titulo: string
  migas: Miga[]
  /** true en "editar" (hay un id en la ruta): se carga el detalle antes de mostrar el formulario. */
  editando: boolean
  /** false si el id de la ruta no es un número entero positivo: no se llama al back y se muestra "no encontrado". */
  idValido: boolean
  detalle: UseQueryResult<T>
  rutaLista: string
  /** Texto del botón que vuelve a la lista cuando el detalle no se pudo cargar. */
  textoVolver: string
  formularioNuevo: ReactNode
  formularioEditar: (datos: T) => ReactNode
}

/**
 * Marco de las pantallas "Nuevo" y "Editar" del panel (ejercicios y microcápsulas): el encabezado y, debajo, según
 * el caso, el formulario vacío, el aviso de error con el botón para volver a la lista, "Cargando…" o el formulario
 * con los datos del detalle.
 */
export function PaginaDeFormulario<T>({
  titulo,
  migas,
  editando,
  idValido,
  detalle,
  rutaLista,
  textoVolver,
  formularioNuevo,
  formularioEditar,
}: PaginaDeFormularioProps<T>) {
  const encabezado = <EncabezadoPagina titulo={titulo} migas={migas} />

  if (!editando) {
    return (
      <>
        {encabezado}
        {formularioNuevo}
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
            <Link to={rutaLista} className={buttonVariants({ variant: "outline" })}>
              {textoVolver}
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

  return (
    <>
      {encabezado}
      {formularioEditar(detalle.data)}
    </>
  )
}
