import almaSaludo from "@/assets/alma_acompana_saludo.webp"
import { ESTADO_CARGANDO } from "@/lib/mensajes"

import { SALUDO_SIN_CARGAR } from "../mensajes"
import { useSaludo } from "../useSaludo"

/**
 * Saludo inicial de Alma (Figma): la ilustración a la izquierda, y a su lado el saludo como título y
 * el mensaje. Mientras carga muestra "Cargando…". Si no se pudo cargar, muestra "Hola, soy Alma" sin
 * mensaje y sin aviso de error: el chat sigue funcionando (11.2).
 */
export function SaludoDeAlma() {
  const saludo = useSaludo()

  return (
    <div className="flex items-center gap-4 lg:gap-6">
      <img
        src={almaSaludo}
        alt=""
        className="h-[calc(132rem/17)] w-auto shrink-0 object-contain lg:h-[calc(145rem/17)]"
      />
      <div className="flex min-w-0 flex-col gap-1">
        {saludo.isPending ? (
          <p role="status" className="text-muted-foreground">
            {ESTADO_CARGANDO}
          </p>
        ) : (
          <>
            <h1 className="lg:text-h1-desktop">{saludo.data?.saludo ?? SALUDO_SIN_CARGAR}</h1>
            {saludo.data ? <p className="text-foreground lg:text-muted-foreground">{saludo.data.mensaje}</p> : null}
          </>
        )}
      </div>
    </div>
  )
}
