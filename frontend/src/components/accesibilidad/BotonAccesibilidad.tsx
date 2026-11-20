import { HugeiconsIcon } from "@hugeicons/react"

import { PanelAccesibilidad } from "@/components/accesibilidad/PanelAccesibilidad"
import { ICONO_ACCESIBILIDAD } from "@/components/iconos"
import { Dialog, DialogTrigger } from "@/components/ui/dialog"
import { usePanelAccesibilidad } from "@/features/accesibilidad/usePanelAccesibilidad"
import { ACCESIBILIDAD_OPCIONES, ACCESIBILIDAD_TITULO } from "@/lib/mensajes"
import { cn } from "@/lib/utils"

// Aspecto común de las tres variantes: ícono con borde accent y fondo card (Figma, 22:55). En alto contraste,
// borde y color del texto.
const BASE =
  "inline-flex items-center justify-center border-accent bg-card text-accent transition-colors hover:bg-accent-soft contraste-alto:border-foreground contraste-alto:text-foreground"
const TEXTO = "font-heading text-button font-medium"

type Variante = "flotante" | "barra" | "menu"

/**
 * Botón de accesibilidad (CU-13). Tres variantes:
 * - `flotante`: botón con su propio panel, para las pantallas que no usan el layout de la persona mayor (Login y
 *   panel del operador). Zona clicable de 44 px (el Figma lo dibuja de 40 y CLAUDE.md pide 44 como mínimo). Fondo
 *   `card`: en celular queda sobre el degradé de la banda del login y sin relleno no se vería el ícono. Arriba a la
 *   derecha en celular, y se desplaza con la pantalla (`absolute`): fijo se superponía con el contenido al
 *   desplazarse con letra "Muy grande". En escritorio, fijo abajo a la derecha.
 * - `barra` (celular) y `menu` (escritorio): para las pantallas de la persona mayor, donde LayoutPersona lo monta
 *   dos veces y el CSS muestra solo una según el ancho. `barra` es un círculo de 44 px en la barra de arriba, en el
 *   flujo de la pantalla (con letra "Muy grande" un botón fijo se superponía con el contenido); `menu` es un botón
 *   ancho con ícono y texto al pie del menú lateral. Abren el panel que monta PanelAccesibilidadProvider.
 *   `ocultoEnCelular` esconde el de la barra (Perfil ya tiene la fila "Tamaño de letra y contraste").
 */
export function BotonAccesibilidad({
  variante,
  ocultoEnCelular = false,
}: {
  variante: Variante
  ocultoEnCelular?: boolean
}) {
  return variante === "flotante" ? (
    <BotonFlotante />
  ) : (
    <BotonDelLayout variante={variante} ocultoEnCelular={ocultoEnCelular} />
  )
}

function BotonFlotante() {
  return (
    <Dialog>
      <DialogTrigger
        aria-label={ACCESIBILIDAD_OPCIONES}
        aria-haspopup="dialog"
        className={cn(
          BASE,
          "absolute top-(--page-top) right-(--page-x) z-40 size-(--tactil) rounded-full border-2 lg:fixed lg:top-auto lg:right-6 lg:bottom-6"
        )}
      >
        <HugeiconsIcon icon={ICONO_ACCESIBILIDAD} className="size-6" aria-hidden="true" />
      </DialogTrigger>
      <PanelAccesibilidad />
    </Dialog>
  )
}

function BotonDelLayout({
  variante,
  ocultoEnCelular,
}: {
  variante: "barra" | "menu"
  ocultoEnCelular: boolean
}) {
  const { abrir } = usePanelAccesibilidad()
  const enBarra = variante === "barra"

  return (
    <button
      type="button"
      aria-label={ACCESIBILIDAD_OPCIONES}
      aria-haspopup="dialog"
      onClick={abrir}
      className={cn(
        BASE,
        TEXTO,
        enBarra
          ? "size-(--tactil) shrink-0 rounded-full border-2 lg:hidden"
          : "max-lg:hidden lg:min-h-(--altura-boton) lg:w-full lg:gap-2 lg:rounded-md lg:border-2 lg:px-3.5",
        enBarra && ocultoEnCelular && "max-lg:hidden"
      )}
    >
      <HugeiconsIcon icon={ICONO_ACCESIBILIDAD} className="size-6 shrink-0" aria-hidden="true" />
      {enBarra ? null : <span>{ACCESIBILIDAD_TITULO}</span>}
    </button>
  )
}
