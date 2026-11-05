import { useEffect, useRef } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Bookmark02Icon, Note01Icon } from "@hugeicons/core-free-icons"
import type { UseQueryResult } from "@tanstack/react-query"
import { useSearchParams } from "react-router-dom"

import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsPanel, TabsTab } from "@/components/ui/tabs"
import { RUTA_ENTRETENIMIENTO } from "@/features/entretenimiento/rutas"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO, NAVEGACION_ENTRETENIMIENTO } from "@/lib/mensajes"

import { TarjetaLectura } from "../components/TarjetaLectura"
import {
  PESTANIA_GUARDADAS,
  PESTANIA_TODAS,
  SIN_GUARDADAS,
  SIN_LECTURAS,
  TITULO_LEER,
} from "../mensajes"
import { PARAMETRO_PESTANIA, pestaniaDe, type Pestania } from "../pestanias"
import type { LecturaResumen } from "../tipos"
import { useGuardadas, useLecturas } from "../useLecturas"

/**
 * Leer (contrato, 10.2; Figma 30:561 y 46:1062): volver a Entretenimiento, el título y las
 * pestañas "Todas" y "Guardadas" (no están en el Figma). La pestaña viaja en la URL
 * (?ver=guardadas) y se cambia con replace: el "atrás" del navegador no recorre las pestañas.
 * Base UI monta solo el panel activo, así que cada lista se pide al abrir su pestaña.
 */
export function LecturasPage() {
  const [parametros, setParametros] = useSearchParams()
  const pestania = pestaniaDe(parametros)

  const tituloRef = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    tituloRef.current?.focus({ preventScroll: true })
  }, [])

  function cambiarPestania(valor: Pestania) {
    setParametros(
      (anteriores) => {
        const nuevos = new URLSearchParams(anteriores)
        if (valor === "guardadas") nuevos.set(PARAMETRO_PESTANIA, "guardadas")
        else nuevos.delete(PARAMETRO_PESTANIA)
        return nuevos
      },
      { replace: true }
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <EnlaceVolver to={RUTA_ENTRETENIMIENTO} texto={NAVEGACION_ENTRETENIMIENTO} />
      <h1 ref={tituloRef} tabIndex={-1} className="outline-none lg:text-h1-desktop">
        {TITULO_LEER}
      </h1>

      <Tabs value={pestania} onValueChange={(valor) => cambiarPestania(valor as Pestania)}>
        {/* En celular las pestañas ocupan todo el ancho, como en recordatorios */}
        <TabsList className="max-lg:-mx-(--page-x)">
          <TabsTab value="todas">
            <HugeiconsIcon icon={Note01Icon} className="size-6" aria-hidden="true" />
            {PESTANIA_TODAS}
          </TabsTab>
          <TabsTab value="guardadas">
            <HugeiconsIcon icon={Bookmark02Icon} className="size-6" aria-hidden="true" />
            {PESTANIA_GUARDADAS}
          </TabsTab>
        </TabsList>
        <TabsPanel value="todas">
          <ListaTodas />
        </TabsPanel>
        <TabsPanel value="guardadas">
          <ListaGuardadas />
        </TabsPanel>
      </Tabs>
    </div>
  )
}

function ListaTodas() {
  return <ListaLecturas consulta={useLecturas()} pestania="todas" textoVacio={SIN_LECTURAS} />
}

function ListaGuardadas() {
  return <ListaLecturas consulta={useGuardadas()} pestania="guardadas" textoVacio={SIN_GUARDADAS} />
}

/** Estados de una lista: cargando, error con "Probar de nuevo", vacía y las tarjetas. */
function ListaLecturas({
  consulta,
  pestania,
  textoVacio,
}: {
  consulta: UseQueryResult<LecturaResumen[]>
  pestania: Pestania
  textoVacio: string
}) {
  if (consulta.isPending) {
    return (
      <p role="status" className="text-muted-foreground">
        {ESTADO_CARGANDO}
      </p>
    )
  }

  if (consulta.isError) {
    return (
      <div className="flex flex-col gap-4">
        <Alert variant="error">{mensajeDeError(consulta.error)}</Alert>
        <Button variant="outline" className="self-start" onClick={() => consulta.refetch()}>
          {ACCION_PROBAR_DE_NUEVO}
        </Button>
      </div>
    )
  }

  if (consulta.data.length === 0) {
    return <Alert variant="info">{textoVacio}</Alert>
  }

  return (
    <ul className="grid gap-4 lg:grid-cols-3 lg:gap-6">
      {consulta.data.map((lectura) => (
        <li key={lectura.id}>
          <TarjetaLectura lectura={lectura} pestania={pestania} />
        </li>
      ))}
    </ul>
  )
}
