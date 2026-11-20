import { useId } from "react"

import { OpcionRadio, TildeOpcion } from "@/components/formularios/OpcionRadio"
import { Button } from "@/components/ui/button"
import {
  DialogBody,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { RadioGroup } from "@/components/ui/radio-group"
import type { TamanioLetra } from "@/features/accesibilidad/tipos"
import { useAccesibilidad } from "@/features/accesibilidad/useAccesibilidad"
import {
  ACCESIBILIDAD_TAMANIO_NORMAL,
  ACCESIBILIDAD_TAMANIO_GRANDE,
  ACCESIBILIDAD_TAMANIO_MUY_GRANDE,
  ACCESIBILIDAD_TITULO,
  ACCESIBILIDAD_TAMANIO,
  ACCESIBILIDAD_AHORA,
  ACCESIBILIDAD_CONTRASTE,
  ACCESIBILIDAD_NORMAL,
  ACCESIBILIDAD_NORMAL_AYUDA,
  ACCESIBILIDAD_ALTO_CONTRASTE,
  ACCESIBILIDAD_ALTO_CONTRASTE_AYUDA,
  ACCESIBILIDAD_MUESTRA_TITULO,
  ACCESIBILIDAD_MUESTRA,
  ACCESIBILIDAD_VOLVER_A_LO_ORIGINAL,
  ACCESIBILIDAD_LISTO,
} from "@/lib/mensajes"

// Tamaños de letra del Figma: 100, 125 y 150 %.
// PROPUESTO: los nombres accesibles de cada opción ("Tamaño normal, 100 %", etc.).
const TAMANIOS: { valor: TamanioLetra; porcentaje: string; nombre: string }[] = [
  { valor: "NORMAL", porcentaje: "100 %", nombre: ACCESIBILIDAD_TAMANIO_NORMAL },
  { valor: "GRANDE", porcentaje: "125 %", nombre: ACCESIBILIDAD_TAMANIO_GRANDE },
  { valor: "MUY_GRANDE", porcentaje: "150 %", nombre: ACCESIBILIDAD_TAMANIO_MUY_GRANDE },
]

/**
 * Panel de opciones de accesibilidad (CU-13): el contenido del diálogo, sin botón propio.
 * Va dentro de un <Dialog>: el Login lo abre con su botón flotante (BotonAccesibilidad) y las
 * pantallas de la persona mayor, con un solo panel montado en el layout (PanelAccesibilidadProvider),
 * que abren el botón del menú y la fila "Tamaño de letra y contraste" de Perfil.
 *
 * El Figma lo dibuja como pantalla completa con "Volver"; el CU-13 habla de un panel que se abre
 * desde el botón, así que acá es un diálogo con el mismo contenido.
 * - Cada cambio se aplica al instante y se guarda (ver useAccesibilidad). El CU-13 dice
 *   "al cerrar el panel", pero se decidió guardar con cada cambio.
 * - CU-13, camino alterno Nº1 ("restablecer valores por defecto"): el botón usa el texto
 *   del Figma, "Volver a lo original".
 * - Los dos botones del pie quedan siempre a la vista; el contenido se desplaza por dentro.
 */
export function PanelAccesibilidad(props: React.ComponentProps<typeof DialogContent>) {
  const { preferencias, cambiar, restablecer } = useAccesibilidad()
  const idTamanio = useId()
  const idContraste = useId()

  const tamanioActual = TAMANIOS.find((t) => t.valor === preferencias.tamanioLetra)

  return (
    <DialogContent {...props}>
      <DialogHeader>
        <DialogTitle>{ACCESIBILIDAD_TITULO}</DialogTitle>
      </DialogHeader>

      <DialogBody className="flex flex-col gap-3">
        <div className="flex items-start justify-between gap-2">
          <h3 id={idTamanio}>{ACCESIBILIDAD_TAMANIO}</h3>
          <p className="shrink-0 text-muted-foreground">
            {ACCESIBILIDAD_AHORA} {tamanioActual?.porcentaje}
          </p>
        </div>
        <RadioGroup
          aria-labelledby={idTamanio}
          value={preferencias.tamanioLetra}
          onValueChange={(valor) => cambiar({ tamanioLetra: valor as TamanioLetra })}
          className="grid-cols-3"
        >
          {TAMANIOS.map((tamanio) => (
            <OpcionRadio
              key={tamanio.valor}
              value={tamanio.valor}
              aria-label={tamanio.nombre}
              className="items-center gap-1 px-2 py-3"
            >
              <span aria-hidden="true" className="font-heading text-h2 font-medium">
                Aa
              </span>
              <span aria-hidden="true" className="font-semibold">
                {tamanio.porcentaje}
              </span>
              <TildeOpcion className="size-5" />
            </OpcionRadio>
          ))}
        </RadioGroup>

        <h3 id={idContraste}>{ACCESIBILIDAD_CONTRASTE}</h3>
        <RadioGroup
          aria-labelledby={idContraste}
          value={preferencias.altoContraste ? "alto" : "normal"}
          onValueChange={(valor) => cambiar({ altoContraste: valor === "alto" })}
        >
          <OpcionRadio value="normal" className="items-start gap-2 p-4">
            {/* Muestra fija (tokens --muestra-*): no cambia con el modo elegido */}
            <span
              aria-hidden="true"
              className="flex h-14 w-25 items-center justify-center rounded-sm border-2 border-muestra-normal-borde bg-muestra-normal-fondo font-heading text-h2 font-medium text-muestra-normal-texto"
            >
              Aa
            </span>
            <span className="flex w-full items-center gap-2">
              <span className="flex-1 font-bold">{ACCESIBILIDAD_NORMAL}</span>
              <TildeOpcion className="size-6" />
            </span>
            <span className="text-muted-foreground">{ACCESIBILIDAD_NORMAL_AYUDA}</span>
          </OpcionRadio>
          <OpcionRadio value="alto" className="items-start gap-2 p-4">
            <span
              aria-hidden="true"
              className="flex h-14 w-25 items-center justify-center rounded-sm border-3 border-muestra-alto-borde bg-muestra-alto-fondo font-heading text-h2 font-medium text-muestra-alto-texto"
            >
              Aa
            </span>
            <span className="flex w-full items-center gap-2">
              <span className="flex-1 font-bold">{ACCESIBILIDAD_ALTO_CONTRASTE}</span>
              <TildeOpcion className="size-6" />
            </span>
            <span className="text-muted-foreground">{ACCESIBILIDAD_ALTO_CONTRASTE_AYUDA}</span>
          </OpcionRadio>
        </RadioGroup>

        <h3>{ACCESIBILIDAD_MUESTRA_TITULO}</h3>
        <p className="rounded-md bg-secondary p-4 text-secondary-foreground">
          {ACCESIBILIDAD_MUESTRA}
        </p>
      </DialogBody>

      <DialogFooter>
        <Button variant="outline" onClick={restablecer}>
          {ACCESIBILIDAD_VOLVER_A_LO_ORIGINAL}
        </Button>
        <DialogClose render={<Button />}>{ACCESIBILIDAD_LISTO}</DialogClose>
      </DialogFooter>
    </DialogContent>
  )
}
