import { HugeiconsIcon } from "@hugeicons/react"
import { AccessibilityIcon } from "@hugeicons/core-free-icons"

import { usePanelAccesibilidad } from "@/features/accesibilidad/usePanelAccesibilidad"
import { cn } from "cn"

/**
 * Botón de accesibilidad de las pantallas de la persona mayor (CU-13). Es un solo elemento:
 * - Celular: círculo flotante arriba a la derecha (como el Figma, 22:55), con zona de 44 px.
 * - Escritorio: botón ancho al pie del menú lateral, con ícono y el texto "Accesibilidad".
 * Abre el panel que monta PanelAccesibilidadProvider. `ocultoEnCelular` lo esconde solo en
 * celular (Perfil ya tiene la fila "Tamaño de letra y contraste" que abre el mismo panel).
 */
export function BotonAccesibilidadMenu({ ocultoEnCelular = false }: { ocultoEnCelular?: boolean }) {
  const { abrir } = usePanelAccesibilidad()

  return (
    <button
      type="button"
      aria-label="Opciones de accesibilidad"
      aria-haspopup="dialog"
      onClick={abrir}
      className={cn(
        "fixed top-(--page-top) right-(--page-x) z-40 inline-flex size-[calc(44rem/17)] items-center justify-center rounded-full border-2 border-accent bg-card font-heading text-button font-medium text-accent transition-colors hover:bg-accent-soft contraste-alto:border-foreground contraste-alto:text-foreground",
        "lg:static lg:mx-4 lg:mt-auto lg:mb-4 lg:size-auto lg:min-h-(--altura-boton) lg:gap-2 lg:rounded-md lg:px-3.5",
        ocultoEnCelular && "max-lg:hidden"
      )}
    >
      <HugeiconsIcon icon={AccessibilityIcon} className="size-6 shrink-0" aria-hidden="true" />
      <span className="hidden lg:inline">Accesibilidad</span>
    </button>
  )
}
