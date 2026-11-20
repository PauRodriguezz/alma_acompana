import { HugeiconsIcon } from "@hugeicons/react"
import { ICONO_ACCESIBILIDAD } from "@/components/alma/iconos"

import { usePanelAccesibilidad } from "@/features/accesibilidad/usePanelAccesibilidad"
import { cn } from "@/lib/utils"

/**
 * Botón de accesibilidad de las pantallas de la persona mayor (CU-13). LayoutPersona lo monta dos veces,
 * una por lugar, y el CSS muestra solo una según el ancho:
 * - `barra` (celular): círculo de 44 px en la barra de arriba, a la derecha de Alma (Figma, 22:55). Va en el
 *   flujo de la pantalla, así que se desplaza con ella y no tapa el contenido (con letra "Muy grande" un botón
 *   fijo se superponía con títulos, tarjetas y botones). La misma opción está siempre a mano en Perfil
 *   ("Tamaño de letra y contraste").
 * - `menu` (escritorio): botón ancho al pie del menú lateral, con ícono y el texto "Accesibilidad".
 * Abre el panel que monta PanelAccesibilidadProvider. `ocultoEnCelular` esconde el de la barra (Perfil ya
 * tiene la fila "Tamaño de letra y contraste" que abre el mismo panel).
 */
export function BotonAccesibilidadMenu({
  ubicacion,
  ocultoEnCelular = false,
}: {
  ubicacion: "barra" | "menu"
  ocultoEnCelular?: boolean
}) {
  const { abrir } = usePanelAccesibilidad()
  const enBarra = ubicacion === "barra"

  return (
    <button
      type="button"
      aria-label="Opciones de accesibilidad"
      aria-haspopup="dialog"
      onClick={abrir}
      className={cn(
        "inline-flex items-center justify-center border-accent bg-card font-heading text-button font-medium text-accent transition-colors hover:bg-accent-soft contraste-alto:border-foreground contraste-alto:text-foreground",
        enBarra
          ? "size-[calc(44rem/17)] shrink-0 rounded-full border-2 lg:hidden"
          : "max-lg:hidden lg:min-h-(--altura-boton) lg:w-full lg:gap-2 lg:rounded-md lg:border-2 lg:px-3.5",
        enBarra && ocultoEnCelular && "max-lg:hidden"
      )}
    >
      <HugeiconsIcon icon={ICONO_ACCESIBILIDAD} className="size-6 shrink-0" aria-hidden="true" />
      {enBarra ? null : <span>Accesibilidad</span>}
    </button>
  )
}
