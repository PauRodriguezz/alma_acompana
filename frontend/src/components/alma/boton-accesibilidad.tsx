import { HugeiconsIcon } from "@hugeicons/react"
import { AccessibilityIcon } from "@hugeicons/core-free-icons"

import { PanelAccesibilidad } from "@/components/alma/panel-accesibilidad"
import { Dialog, DialogTrigger } from "@/components/ui/dialog"

/**
 * Botón flotante de accesibilidad (CU-13) con su panel, para pantallas que no usan el layout de
 * la persona mayor (hoy, el Login). En las pantallas de la persona mayor el botón y el panel
 * viven en el layout (ver LayoutPersona y PanelAccesibilidadProvider). El contenido del panel
 * está en PanelAccesibilidad.
 *
 * Botón: ícono de accesibilidad con borde accent, como el del Figma (22:55).
 * - Desviación a propósito: zona clicable de 44 px; el Figma lo dibuja de 40 px y CLAUDE.md
 *   pide 44 px como mínimo.
 * - PROPUESTO: fondo `card` (el Figma no tiene relleno): en celular queda sobre el degradé
 *   de la banda del login y sin relleno no se vería el ícono.
 * - Posición: arriba a la derecha en celular, como el Figma. PROPUESTO (no está en el
 *   Figma): abajo a la derecha en escritorio.
 */
export function BotonAccesibilidad() {
  return (
    <Dialog>
      <DialogTrigger
        aria-label="Opciones de accesibilidad"
        aria-haspopup="dialog"
        className="fixed top-(--page-top) right-(--page-x) z-40 inline-flex size-[calc(44rem/17)] items-center justify-center rounded-full border-2 border-accent bg-card text-accent transition-colors hover:bg-accent-soft lg:top-auto lg:right-6 lg:bottom-6 contraste-alto:border-foreground contraste-alto:text-foreground"
      >
        <HugeiconsIcon icon={AccessibilityIcon} className="size-6" aria-hidden="true" />
      </DialogTrigger>
      <PanelAccesibilidad />
    </Dialog>
  )
}
