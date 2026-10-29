import { Badge } from "@/components/ui/badge"
import {
  ESTILO_CATEGORIA_EJERCICIO,
  TEXTO_CATEGORIA_EJERCICIO,
  type CategoriaEjercicio,
} from "@/features/admin/etiquetas"

/** Etiqueta de categoría de ejercicio (Figma 39:276): fondo game-*-soft y un punto del color del juego. */
export function EtiquetaCategoriaEjercicio({ categoria }: { categoria: CategoriaEjercicio }) {
  const estilo = ESTILO_CATEGORIA_EJERCICIO[categoria]
  return (
    <Badge className={estilo.fondo}>
      <span aria-hidden="true" className={`size-2.5 shrink-0 rounded-full ${estilo.punto}`} />
      {TEXTO_CATEGORIA_EJERCICIO[categoria]}
    </Badge>
  )
}
