import type { ReactNode } from "react"
import { Link } from "react-router-dom"

import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { TituloConFoco } from "@/components/alma/titulo-con-foco"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"
import {
  ACCION_PROBAR_DE_NUEVO,
  ESTADO_CARGANDO,
  MENSAJE_NO_ENCONTRADO,
  NAVEGACION_HISTORIA_DE_VIDA,
} from "@/lib/mensajes"

import { formatearAnio } from "../anio"
import { CON_QUIEN, DONDE, respondisteA, TITULO_HISTORIA_DE_VIDA } from "../mensajes"
import { RUTA_HISTORIA_DE_VIDA } from "../rutas"
import type { RecuerdoDetalle } from "../tipos"
import { useRecuerdo } from "../useHistoria"
import { useIdDeRecuerdo } from "../useIdDeRecuerdo"
import { useSeguirCompletando } from "../useSeguirCompletando"
import { BotonSeguirCompletando } from "../components/BotonSeguirCompletando"

// Ancho de lectura (PROPUESTO, no está en el Figma): unos 70 caracteres por línea.
const ANCHO_LECTURA = "max-w-[calc(680rem/17)]"

function esNoEncontrado(error: unknown) {
  return error instanceof ApiError && error.status === 404
}

/**
 * Detalle de un recuerdo (CU-04; no está en el Figma): "‹ Historia de vida", título, año, descripción,
 * "Con quién" y "Dónde" (solo si hay). Un id con formato inválido no llama al back.
 */
export function RecuerdoDetallePage() {
  const id = useIdDeRecuerdo()
  const consulta = useRecuerdo(id)

  let contenido: ReactNode
  if (id === undefined || (consulta.isError && esNoEncontrado(consulta.error))) {
    contenido = (
      <>
        <TituloConFoco>{TITULO_HISTORIA_DE_VIDA}</TituloConFoco>
        <Alert variant="info">{MENSAJE_NO_ENCONTRADO}</Alert>
        <Link
          to={RUTA_HISTORIA_DE_VIDA}
          className={buttonVariants({ variant: "outline", className: "self-start" })}
        >
          {NAVEGACION_HISTORIA_DE_VIDA}
        </Link>
      </>
    )
  } else if (consulta.isError) {
    contenido = (
      <>
        <TituloConFoco>{TITULO_HISTORIA_DE_VIDA}</TituloConFoco>
        <Alert variant="error">{mensajeDeError(consulta.error)}</Alert>
        <Button variant="outline" className="self-start" onClick={() => consulta.refetch()}>
          {ACCION_PROBAR_DE_NUEVO}
        </Button>
      </>
    )
  } else if (consulta.isPending) {
    contenido = (
      <p role="status" className="text-muted-foreground">
        {ESTADO_CARGANDO}
      </p>
    )
  } else {
    contenido = <Recuerdo recuerdo={consulta.data} />
  }

  return (
    <div className={`flex flex-col gap-4 ${ANCHO_LECTURA}`}>
      <EnlaceVolver to={RUTA_HISTORIA_DE_VIDA} texto={NAVEGACION_HISTORIA_DE_VIDA} />
      {contenido}
    </div>
  )
}

function Recuerdo({ recuerdo }: { recuerdo: RecuerdoDetalle }) {
  const anio = formatearAnio(recuerdo.precision, recuerdo.anio)
  const completando = useSeguirCompletando()
  return (
    <article className="flex flex-col gap-4">
      <TituloConFoco>{recuerdo.titulo}</TituloConFoco>
      {recuerdo.preguntaTexto ? (
        <p className="text-muted-foreground wrap-break-word">{respondisteA(recuerdo.preguntaTexto)}</p>
      ) : null}
      {anio ? <p className="font-semibold text-heading">{anio}</p> : null}
      <p className="text-body wrap-break-word whitespace-pre-line">{recuerdo.descripcion}</p>

      {completando.aviso ? <Alert variant="info">{completando.aviso}</Alert> : null}
      {completando.error ? <Alert variant="error">{completando.error}</Alert> : null}
      {recuerdo.faltantes.length > 0 ? (
        <BotonSeguirCompletando
          onClick={() => void completando.completar(recuerdo.id)}
          esperando={completando.esperandoId === recuerdo.id}
          deshabilitado={completando.ocupado}
        />
      ) : null}

      {recuerdo.personas.length > 0 ? (
        <section aria-labelledby="recuerdo-personas" className="flex flex-col gap-1">
          <h2 id="recuerdo-personas" className="text-h3">
            {CON_QUIEN}
          </h2>
          <ul>
            {recuerdo.personas.map((persona) => (
              <li key={persona.id} className="wrap-break-word">
                {persona.nombre}
                {persona.vinculo ? ` (${persona.vinculo})` : ""}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {recuerdo.lugares.length > 0 ? (
        <section aria-labelledby="recuerdo-lugares" className="flex flex-col gap-1">
          <h2 id="recuerdo-lugares" className="text-h3">
            {DONDE}
          </h2>
          <ul>
            {recuerdo.lugares.map((lugar) => (
              <li key={lugar.id} className="wrap-break-word">
                {lugar.nombre}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </article>
  )
}
