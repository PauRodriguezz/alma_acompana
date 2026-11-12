import { useRef, type CSSProperties } from "react"
import { Dialog } from "@base-ui/react/dialog"
import { HugeiconsIcon } from "@hugeicons/react"
import { ArrowRight01Icon, Cancel01Icon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

import { formatearAnio } from "../anio"
import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import { variablesDeEtapa } from "../etapas"
import { CERRAR_PANEL, PANEL_SIN_RECUERDOS } from "../mensajes"
import { rutaDeRecuerdo } from "../rutas"
import { useSeguirCompletando } from "../useSeguirCompletando"
import { BotonSeguirCompletando } from "./BotonSeguirCompletando"

type Props = {
  grupo: EtapaConRecuerdos
  /** Hoja que sube desde abajo (celular) o columna fija (escritorio, desde `lg`). */
  modo: "hoja" | "columna"
  /** Solo en la hoja: se llama al cerrarla (botón, Escape o tocar fuera). */
  onCerrar?: () => void
}

/** Busca la parada (o tarjeta de la lista) de la etapa para devolverle el foco al cerrar la hoja. */
function paradaDe(id: string) {
  return document.querySelector<HTMLElement>(`[data-etapa="${id}"]`)
}

/**
 * Panel de la etapa elegida (contrato, 2.3). En celular es una hoja con `role="dialog"`: el foco pasa al título
 * al abrir, queda adentro mientras está abierta y vuelve a la parada al cerrar. En escritorio es una columna.
 */
export function PanelDeEtapa({ grupo, modo, onCerrar }: Props) {
  const tituloRef = useRef<HTMLHeadingElement>(null)
  const { etapa, recuerdos } = grupo
  const completando = useSeguirCompletando()

  const encabezado = (
    <div className="flex items-start gap-3" style={variablesDeEtapa(etapa) as CSSProperties}>
      <span
        aria-hidden="true"
        className="mt-2 size-3.5 shrink-0 rounded-full bg-(--etapa-fuerte) contraste-alto:bg-foreground"
      />
      <div className="min-w-0 flex-1">
        {modo === "hoja" ? (
          <Dialog.Title
            ref={tituloRef}
            tabIndex={-1}
            className="font-heading font-medium text-heading outline-none"
          >
            {etapa.nombre}
          </Dialog.Title>
        ) : (
          <h2 className="font-heading font-medium text-heading">{etapa.nombre}</h2>
        )}
        <p className="text-muted-foreground">{etapa.rango}</p>
      </div>
      {modo === "hoja" ? (
        <Dialog.Close
          aria-label={CERRAR_PANEL}
          render={<Button variant="secondary" className="size-(--altura-boton) shrink-0 p-0" />}
        >
          <HugeiconsIcon icon={Cancel01Icon} className="size-6" aria-hidden="true" />
        </Dialog.Close>
      ) : null}
    </div>
  )

  const avisos = (
    <>
      {completando.aviso ? <Alert variant="info">{completando.aviso}</Alert> : null}
      {completando.error ? <Alert variant="error">{completando.error}</Alert> : null}
    </>
  )

  const lista =
    recuerdos.length === 0 ? (
      <p className="text-muted-foreground">{PANEL_SIN_RECUERDOS}</p>
    ) : (
      <ul className="m-0 flex list-none flex-col p-0">
        {recuerdos.map((recuerdo) => {
          const anio = formatearAnio(recuerdo.precision, recuerdo.anio)
          return (
            <li key={recuerdo.id} className="flex flex-col border-t border-border pb-1 first:border-t-0">
              <Link
                to={rutaDeRecuerdo(recuerdo.id)}
                className="flex min-h-[2.75rem] items-center gap-3 rounded-md py-3"
              >
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="wrap-break-word font-semibold text-heading">{recuerdo.titulo}</span>
                  {anio ? <span className="text-muted-foreground">{anio}</span> : null}
                </span>
                <HugeiconsIcon
                  icon={ArrowRight01Icon}
                  className="size-6 shrink-0 text-muted-foreground"
                  aria-hidden="true"
                />
              </Link>
              {recuerdo.faltantes.length > 0 ? (
                <BotonSeguirCompletando
                  onClick={() => void completando.completar(recuerdo.id)}
                  esperando={completando.esperandoId === recuerdo.id}
                  deshabilitado={completando.ocupado}
                />
              ) : null}
            </li>
          )
        })}
      </ul>
    )

  if (modo === "columna") {
    return (
      <section
        aria-label={etapa.nombre}
        className="flex flex-col gap-4 rounded-xl border-2 border-border bg-card p-5 lg:sticky lg:top-(--page-top)"
      >
        {encabezado}
        {avisos}
        {lista}
      </section>
    )
  }

  return (
    <Dialog.Root open onOpenChange={(abierto) => (abierto ? null : onCerrar?.())}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-foreground/30 transition-opacity duration-150 data-ending-style:opacity-0 data-starting-style:opacity-0" />
        <Dialog.Popup
          initialFocus={tituloRef}
          finalFocus={() => paradaDe(etapa.id)}
          className={cn(
            "fixed inset-x-0 bottom-0 z-50 flex max-h-[55dvh] flex-col gap-4 overflow-y-auto rounded-t-2xl border border-b-0 border-border bg-popover px-5 pt-3 pb-6 text-body outline-none",
            "[box-shadow:var(--sombra-tarjeta)] contraste-alto:border-2 contraste-alto:border-foreground",
            "transition duration-200 ease-out data-ending-style:translate-y-full data-starting-style:translate-y-full"
          )}
        >
          <span aria-hidden="true" className="mx-auto h-1 w-9 shrink-0 rounded-full bg-border contraste-alto:bg-foreground" />
          {encabezado}
          {avisos}
          {lista}
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
