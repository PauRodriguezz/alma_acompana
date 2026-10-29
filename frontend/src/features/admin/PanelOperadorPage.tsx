import { Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"

import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Alert } from "@/components/ui/alert"
import { Card } from "@/components/ui/card"

import { CLAVE_RESUMEN, obtenerResumen } from "./api"
import { EncabezadoPagina } from "./EncabezadoPagina"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO } from "@/lib/mensajes"
import {
  ADMIN_EJERCICIOS,
  ADMIN_MICROCAPSULAS,
  ADMIN_NUEVA_MICROCAPSULA,
  ADMIN_NUEVO_EJERCICIO,
  ADMIN_PANEL,
  ADMIN_VER_EJERCICIOS,
  ADMIN_VER_MICROCAPSULAS,
} from "./mensajes"

type TarjetaConteoProps = {
  titulo: string
  cantidad: number | undefined
  cargando: boolean
  ver: { texto: string; a: string }
  nuevo: { texto: string; a: string }
}

/**
 * Tarjeta del Figma (51:1473): la cifra en h1-desktop, el nombre en negrita y los botones
 * "Ver" (outline) y "Nuevo" (primary). El nombre es un h2: en el código va antes que la
 * cifra (el lector dice "Ejercicios, 48") y en pantalla se ve debajo, como en el Figma.
 */
function TarjetaConteo({ titulo, cantidad, cargando, ver, nuevo }: TarjetaConteoProps) {
  return (
    <Card className="flex-1 gap-4 p-8">
      <div className="flex flex-col-reverse gap-1">
        <h2 className="font-sans text-body font-bold text-foreground">{titulo}</h2>
        {cargando ? (
          <p role="status" className="text-muted-foreground">
            {ESTADO_CARGANDO}
          </p>
        ) : (
          <p className="font-heading text-h1 font-medium text-heading lg:text-h1-desktop">
            {cantidad ?? "—"}
          </p>
        )}
      </div>
      <div className="flex flex-wrap gap-3">
        <Link to={ver.a} className={buttonVariants({ variant: "outline" })}>
          {ver.texto}
        </Link>
        <Link to={nuevo.a} className={buttonVariants()}>
          {nuevo.texto}
        </Link>
      </div>
    </Card>
  )
}

/** Panel de operador (Figma 51:1409): conteos de GET /api/admin/panel/resumen. */
export function PanelOperadorPage() {
  const resumen = useQuery({ queryKey: CLAVE_RESUMEN, queryFn: obtenerResumen })

  return (
    <>
      <EncabezadoPagina titulo={ADMIN_PANEL} />

      {resumen.isError ? (
        <Alert variant="error">
          <div className="flex flex-col items-start gap-3">
            <p>{mensajeDeError(resumen.error)}</p>
            <Button
              variant="outline"
              onClick={() => resumen.refetch()}
            >
              {ACCION_PROBAR_DE_NUEVO}
            </Button>
          </div>
        </Alert>
      ) : null}

      <div className="flex flex-col gap-6 @xl:flex-row">
        <TarjetaConteo
          titulo={ADMIN_EJERCICIOS}
          cantidad={resumen.data?.ejerciciosActivos}
          cargando={resumen.isPending && !resumen.isError}
          ver={{ texto: ADMIN_VER_EJERCICIOS, a: "/admin/ejercicios" }}
          nuevo={{ texto: ADMIN_NUEVO_EJERCICIO, a: "/admin/ejercicios/nuevo" }}
        />
        <TarjetaConteo
          titulo={ADMIN_MICROCAPSULAS}
          cantidad={resumen.data?.microcapsulasVigentes}
          cargando={resumen.isPending && !resumen.isError}
          ver={{ texto: ADMIN_VER_MICROCAPSULAS, a: "/admin/microcapsulas" }}
          nuevo={{ texto: ADMIN_NUEVA_MICROCAPSULA, a: "/admin/microcapsulas/nueva" }}
        />
      </div>
    </>
  )
}
