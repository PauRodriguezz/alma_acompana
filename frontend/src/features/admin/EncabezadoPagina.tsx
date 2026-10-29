import { Fragment, useEffect, type ReactNode } from "react"
import { Link } from "react-router-dom"

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"

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
        <h1 className="min-w-0 lg:text-h1-desktop">{titulo}</h1>
        {accion}
      </div>
    </div>
  )
}
