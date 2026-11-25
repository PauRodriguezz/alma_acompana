import almaSaludo from "@/assets/alma_acompana_saludo.webp"
import { ESTADO_CARGANDO } from "@/lib/mensajes"

import { SALUDO_SIN_CARGAR } from "../mensajes"
import { useSaludo } from "../useSaludo"

/**
 * Saludo inicial de Alma (Figma): la ilustración a la izquierda, y a su lado el saludo como título y
 * el mensaje. La ilustración es decorativa y mide un tamaño fijo en px (no en rem): con letra "Muy
 * grande" crecía hasta dejar 83 px al texto y el título se salía de la pantalla a 360 px. El texto sí
 * escala (excepción documentada en el contrato del chat, sección 11). Mientras carga muestra "Cargando…". Si no se pudo cargar, muestra "Hola, soy Alma" sin
 * mensaje y sin aviso de error: el chat sigue funcionando (11.2).
 */
export function SaludoDeAlma() {
  const saludo = useSaludo()

  return (
    <div className="flex items-center gap-4 lg:gap-6">
      <img
        src={almaSaludo}
        alt=""
        className="h-[132px] w-auto shrink-0 object-contain lg:h-[145px]"
      />
      <div className="flex min-w-0 flex-col gap-1">
        {saludo.isPending ? (
          <p role="status" className="text-muted-foreground">
            {ESTADO_CARGANDO}
          </p>
        ) : (
          <>
            <h1>{saludo.data?.saludo ?? SALUDO_SIN_CARGAR}</h1>
            {saludo.data ? <p className="text-foreground lg:text-muted-foreground">{saludo.data.mensaje}</p> : null}
          </>
        )}
      </div>
    </div>
  )
}
