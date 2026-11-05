import { Bookmark02Icon } from "@hugeicons/core-free-icons"

import { Chip } from "@/components/alma/chip"

import { MARCA_GUARDADA } from "../mensajes"

/** "Guardada" con ícono y texto (nunca solo el ícono). La usan la tarjeta y el detalle. */
export function MarcaGuardada() {
  return (
    <Chip tono="acento" icono={Bookmark02Icon}>
      {MARCA_GUARDADA}
    </Chip>
  )
}
