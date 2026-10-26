import { useId } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { AccessibilityIcon } from "@hugeicons/core-free-icons"

import { OpcionAccesibilidad, TildeOpcion } from "@/components/alma/opcion-accesibilidad"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { RadioGroup } from "@/components/ui/radio-group"
import { useAccesibilidad } from "@/features/accesibilidad/useAccesibilidad"
import type { TamanioLetra } from "@/features/accesibilidad/tipos"

// Tamaños de letra del Figma: 100, 125 y 150 %.
// PROPUESTO: los nombres accesibles de cada opción ("Tamaño normal, 100 %", etc.).
const TAMANIOS: { valor: TamanioLetra; porcentaje: string; nombre: string }[] = [
  { valor: "NORMAL", porcentaje: "100 %", nombre: "Tamaño normal, 100 %" },
  { valor: "GRANDE", porcentaje: "125 %", nombre: "Tamaño grande, 125 %" },
  { valor: "MUY_GRANDE", porcentaje: "150 %", nombre: "Tamaño muy grande, 150 %" },
]

/**
 * Botón de accesibilidad (CU-13) y su panel de opciones. Es reutilizable: se pone una vez
 * por pantalla. Hoy solo está en el login.
 *
 * Botón: ícono de accesibilidad con borde accent, como el del Figma (22:55).
 * - Desviación a propósito: zona clicable de 44 px; el Figma lo dibuja de 40 px y CLAUDE.md
 *   pide 44 px como mínimo.
 * - PROPUESTO: fondo `card` (el Figma no tiene relleno): en celular queda sobre el degradé
 *   de la banda del login y sin relleno no se vería el ícono.
 * - Posición: arriba a la derecha en celular, como el Figma. PROPUESTO (no está en el
 *   Figma): abajo a la derecha en escritorio.
 *
 * Panel: el Figma lo dibuja como pantalla completa con "Volver"; el CU-13 habla de un panel
 * que se abre desde el botón flotante, así que acá es un diálogo con el mismo contenido.
 * - Cada cambio se aplica al instante y se guarda (ver useAccesibilidad). El CU-13 dice
 *   "al cerrar el panel", pero se decidió guardar con cada cambio.
 * - CU-13, camino alterno Nº1 ("restablecer valores por defecto"): el botón usa el texto
 *   del Figma, "Volver a lo original".
 * - Los dos botones del pie quedan siempre a la vista; el contenido se desplaza por dentro.
 */
export function BotonAccesibilidad() {
  const { preferencias, cambiar, restablecer } = useAccesibilidad()
  const idTamanio = useId()
  const idContraste = useId()

  const tamanioActual = TAMANIOS.find((t) => t.valor === preferencias.tamanioLetra)

  return (
    <Dialog>
      <DialogTrigger
        aria-label="Opciones de accesibilidad"
        aria-haspopup="dialog"
        className="fixed top-(--page-top) right-(--page-x) z-40 inline-flex size-[calc(44rem/17)] items-center justify-center rounded-full border-2 border-accent bg-card text-accent transition-colors hover:bg-accent-soft lg:top-auto lg:right-6 lg:bottom-6 contraste-alto:border-foreground contraste-alto:text-foreground"
      >
        <HugeiconsIcon icon={AccessibilityIcon} className="size-6" aria-hidden="true" />
      </DialogTrigger>

      <DialogContent>
        <DialogHeader>
          <DialogTitle>Accesibilidad</DialogTitle>
        </DialogHeader>

        <DialogBody className="flex flex-col gap-3">
          <div className="flex items-start justify-between gap-2">
            <h3 id={idTamanio}>Tamaño de la letra</h3>
            <p className="shrink-0 text-muted-foreground">
              Ahora: {tamanioActual?.porcentaje}
            </p>
          </div>
          <RadioGroup
            aria-labelledby={idTamanio}
            value={preferencias.tamanioLetra}
            onValueChange={(valor) => cambiar({ tamanioLetra: valor as TamanioLetra })}
            className="grid-cols-3"
          >
            {TAMANIOS.map((tamanio) => (
              <OpcionAccesibilidad
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
              </OpcionAccesibilidad>
            ))}
          </RadioGroup>

          <h3 id={idContraste}>Contraste</h3>
          <RadioGroup
            aria-labelledby={idContraste}
            value={preferencias.altoContraste ? "alto" : "normal"}
            onValueChange={(valor) => cambiar({ altoContraste: valor === "alto" })}
          >
            <OpcionAccesibilidad value="normal" className="items-start gap-2 p-4">
              {/* Muestra fija (colores del Figma): no cambia con el modo elegido */}
              <span
                aria-hidden="true"
                className="flex h-14 w-25 items-center justify-center rounded-sm border-2 border-[#e5e0d2] bg-[#fffcf5] font-heading text-h2 font-medium text-[#2b4150]"
              >
                Aa
              </span>
              <span className="flex w-full items-center gap-2">
                <span className="flex-1 font-bold">Normal</span>
                <TildeOpcion className="size-6" />
              </span>
              <span className="text-muted-foreground">Los colores de siempre</span>
            </OpcionAccesibilidad>
            <OpcionAccesibilidad value="alto" className="items-start gap-2 p-4">
              <span
                aria-hidden="true"
                className="flex h-14 w-25 items-center justify-center rounded-sm border-3 border-black bg-white font-heading text-h2 font-medium text-black"
              >
                Aa
              </span>
              <span className="flex w-full items-center gap-2">
                <span className="flex-1 font-bold">Alto contraste</span>
                <TildeOpcion className="size-6" />
              </span>
              <span className="text-muted-foreground">Letras negras y bordes marcados</span>
            </OpcionAccesibilidad>
          </RadioGroup>

          <h3>Así se va a ver</h3>
          <p className="rounded-md bg-secondary p-4 text-secondary-foreground">
            Hoy tenés una pastilla para tomar a las 21:00.
          </p>
        </DialogBody>

        <DialogFooter>
          <Button variant="outline" onClick={restablecer}>
            Volver a lo original
          </Button>
          <DialogClose render={<Button />}>Listo</DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
