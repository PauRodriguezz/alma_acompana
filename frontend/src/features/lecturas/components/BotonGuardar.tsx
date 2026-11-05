import { HugeiconsIcon } from "@hugeicons/react"
import { BookmarkAdd02Icon, BookmarkRemove02Icon } from "@hugeicons/core-free-icons"

import { Button } from "@/components/ui/button"

import { BOTON_GUARDAR, BOTON_QUITAR } from "../mensajes"

// Con focusableWhenDisabled, Base UI no pone el atributo disabled (pone aria-disabled y
// data-disabled), así que las clases disabled: del botón no se aplican: se repiten acá.
const ASPECTO_DESHABILITADO =
  "data-disabled:pointer-events-none data-disabled:translate-y-0 data-disabled:border-input data-disabled:bg-disabled data-disabled:text-disabled-foreground data-disabled:shadow-none"

/**
 * "Guardar" o "Quitar de guardadas" (contrato, 10.2), con ícono y texto visible. Sin
 * aria-pressed (decisión del equipo): el texto ya dice la acción y el estado se ve en la marca
 * "Guardada" del detalle. Deshabilitado mientras viaja el pedido, pero sin perder el foco
 * (focusableWhenDisabled): con teclado, el foco no se va al principio de la página. Con letra
 * grande el texto puede partirse en dos líneas en celular.
 */
export function BotonGuardar({
  guardada,
  enviando,
  onClick,
}: {
  guardada: boolean
  enviando: boolean
  onClick: () => void
}) {
  return (
    <Button
      variant={guardada ? "outline" : "primary"}
      disabled={enviando}
      focusableWhenDisabled
      onClick={onClick}
      className={`self-start text-center whitespace-normal max-lg:w-full ${ASPECTO_DESHABILITADO}`}
    >
      <HugeiconsIcon
        icon={guardada ? BookmarkRemove02Icon : BookmarkAdd02Icon}
        className="size-6"
        aria-hidden="true"
      />
      {guardada ? BOTON_QUITAR : BOTON_GUARDAR}
    </Button>
  )
}
