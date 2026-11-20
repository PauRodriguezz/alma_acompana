import type { ReactNode } from "react"
import type { IconSvgElement } from "@hugeicons/react"
import { Link } from "react-router-dom"

import { Chip } from "@/components/alma/chip"
import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { ICONO_LUGAR, ICONO_PERSONAS } from "@/components/alma/iconos"
import { TituloConFoco } from "@/components/alma/titulo-con-foco"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import { ALMA_PENSANDO } from "@/features/chat/mensajes"
import { ApiError } from "@/lib/api"
import { cn } from "@/lib/utils"
import { mensajeDeError } from "@/lib/errores"
import {
  ACCION_PROBAR_DE_NUEVO,
  ESTADO_CARGANDO,
  MENSAJE_NO_ENCONTRADO,
  NAVEGACION_HISTORIA_DE_VIDA,
} from "@/lib/mensajes"

import { formatearAnio } from "../anio"
import { etapaDe, variablesDeEtapa } from "../etapas"
import { BOTON_EN_ESPERA, BOTON_TEXTO_LARGO } from "../estilos"
import { CON_QUIEN, DONDE, respondisteA, SEGUIR_COMPLETANDO, TITULO_HISTORIA_DE_VIDA } from "../mensajes"
import { RUTA_HISTORIA_DE_VIDA } from "../rutas"
import type { PersonaDeRecuerdo, RecuerdoDetalle } from "../tipos"
import { useRecuerdo } from "../useHistoria"
import { useIdDeRecuerdo } from "../useIdDeRecuerdo"
import { useSeguirCompletando } from "../useSeguirCompletando"

// Ancho de lectura (PROPUESTO, no está en el Figma): unos 70 caracteres por línea.
const ANCHO_LECTURA = "max-w-[calc(680rem/17)]"

function esNoEncontrado(error: unknown) {
  return error instanceof ApiError && error.status === 404
}

/**
 * Detalle de un recuerdo (CU-04; no está en el Figma): "‹ Historia de vida" y la ficha de la etapa (ver `Recuerdo`). Un id con formato inválido no llama al back.
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

/** Cómo se nombra a una persona del recuerdo: "Nombre (vínculo)"; sin nombre, solo el vínculo ("Amiga"); sin nada, no se muestra. */
function etiquetaDePersona(persona: PersonaDeRecuerdo): string | null {
  const nombre = persona.nombre?.trim() ?? ""
  const vinculo = persona.vinculo?.trim() ?? ""
  if (nombre && vinculo) return `${nombre} (${vinculo})`
  if (nombre) return nombre
  return vinculo ? vinculo.charAt(0).toUpperCase() + vinculo.slice(1) : null
}

/**
 * "Ficha de la etapa": el banderín de la etapa con el año al lado, el título, la descripción en una tarjeta con el
 * borde del color de la etapa (con "Con quién" y "Dónde" como chips con ícono), "Respondiste a…" como cita
 * discreta y el botón de seguir completando. Los colores salen de la etapa; en alto contraste, del texto.
 */
function Recuerdo({ recuerdo }: { recuerdo: RecuerdoDetalle }) {
  const anio = formatearAnio(recuerdo.precision, recuerdo.anio)
  const completando = useSeguirCompletando()
  const etapa = etapaDe(recuerdo.etapaDeVida ?? "OTROS")
  const personas = recuerdo.personas.map(etiquetaDePersona).filter((p): p is string => p !== null)
  const tieneDatos = personas.length > 0 || recuerdo.lugares.length > 0
  return (
    <article className="flex flex-col gap-4" style={variablesDeEtapa(etapa)}>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
        <span
          className="bg-(--etapa-fuerte) py-1 pr-7 pl-3 font-heading text-label font-medium text-white contraste-alto:bg-foreground contraste-alto:text-background"
          style={{ clipPath: "polygon(0 0, 100% 0, calc(100% - 0.75rem) 50%, 100% 100%, 0 100%)" }}
        >
          {etapa.nombre}
        </span>
        {anio ? (
          <span className="font-semibold text-(--etapa-fuerte) contraste-alto:text-foreground">{anio}</span>
        ) : null}
      </div>
      <TituloConFoco>{recuerdo.titulo}</TituloConFoco>

      <Card className="gap-5 border-2 border-(--etapa-claro) max-lg:p-4">
        <p className="text-body wrap-break-word whitespace-pre-line">{recuerdo.descripcion}</p>
        {tieneDatos ? <hr className="border-t-2 border-border contraste-alto:border-foreground" /> : null}
        {personas.length > 0 ? (
          <Datos id="recuerdo-personas" titulo={CON_QUIEN}>
            {personas.map((persona, i) => (
              <ChipDeDato key={`${persona}-${i}`} icono={ICONO_PERSONAS}>
                {persona}
              </ChipDeDato>
            ))}
          </Datos>
        ) : null}
        {recuerdo.lugares.length > 0 ? (
          <Datos id="recuerdo-lugares" titulo={DONDE}>
            {recuerdo.lugares.map((lugar) => (
              <ChipDeDato key={lugar.id} icono={ICONO_LUGAR}>
                {lugar.nombre}
              </ChipDeDato>
            ))}
          </Datos>
        ) : null}
      </Card>

      {recuerdo.preguntaTexto ? (
        <p className="border-l-4 border-border pl-4 text-muted-foreground wrap-break-word contraste-alto:border-foreground">
          {respondisteA(recuerdo.preguntaTexto)}
        </p>
      ) : null}

      {completando.aviso ? <Alert variant="info">{completando.aviso}</Alert> : null}
      {completando.error ? <Alert variant="error">{completando.error}</Alert> : null}
      {recuerdo.faltantes.length > 0 ? (
        <button
          type="button"
          aria-disabled={completando.ocupado}
          onClick={completando.ocupado ? undefined : () => void completando.completar(recuerdo.id)}
          className={cn(buttonVariants(), "self-start", BOTON_TEXTO_LARGO, BOTON_EN_ESPERA)}
        >
          {completando.esperandoId === recuerdo.id ? ALMA_PENSANDO : SEGUIR_COMPLETANDO}
        </button>
      ) : null}
    </article>
  )
}

/** Un grupo de datos de la ficha (`Con quién`, `Dónde`): título y chips. */
function Datos({ id, titulo, children }: { id: string; titulo: string; children: ReactNode }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-2">
      <h2 id={id} className="font-heading text-body font-medium">
        {titulo}
      </h2>
      <ul className="flex flex-wrap gap-2">{children}</ul>
    </section>
  )
}

function ChipDeDato({ icono, children }: { icono: IconSvgElement; children: ReactNode }) {
  return (
    <li className="max-w-full">
      <Chip icono={icono} className="gap-2 border-2 border-(--etapa-claro) bg-card py-1.5 pr-4 pl-3">
        {children}
      </Chip>
    </li>
  )
}
