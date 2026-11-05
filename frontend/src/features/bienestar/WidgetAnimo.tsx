import { useRef } from "react"

import { MensajeAlma } from "@/components/alma/mensaje-alma"
import { Alert } from "@/components/ui/alert"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"

import { CaritaAnimo } from "./CaritaAnimo"
import { ERROR_REGISTRAR_ANIMO, NOMBRE_GRUPO_ANIMO, PREGUNTA_ANIMO } from "./mensajes"
import { NIVELES_ANIMO, type NivelAnimo, type RegistroAnimo } from "./tipos"
import { useGuardarAnimo } from "./useGuardarAnimo"

type Props = {
  /** Sin valor, al elegir se crea un registro. Con el id del registro recién creado, se corrige ese. */
  registroId?: number
  /** El padre recibe el registro (con su pregunta contextual) y decide qué mostrar. Una vez por guardado. */
  onRegistrado: (registro: RegistroAnimo) => void
}

// Para el 401, el 403 y sin conexión se usa el texto general; para el resto, el del módulo (9.1).
function textoDeError(error: unknown): string {
  const generales = [0, 401, 403]
  return error instanceof ApiError && generales.includes(error.status)
    ? mensajeDeError(error)
    : ERROR_REGISTRAR_ANIMO
}

/**
 * Widget de ánimo (CU-01): la pregunta de Alma y cinco caritas. Tocar una carita registra de
 * inmediato, sin confirmación ni comentario. No es una tarjeta ni un modal: es un mensaje más de
 * Alma y no bloquea. No se oculta solo ni muestra la pregunta contextual: eso lo hace el padre
 * (el Chat).
 */
export function WidgetAnimo({ registroId, onRegistrado }: Props) {
  const guardar = useGuardarAnimo({ registroId, onRegistrado })
  // Evita que un doble toque rápido cree dos registros (isPending se actualiza un instante después).
  const guardando = useRef(false)

  function elegir(nivel: NivelAnimo) {
    if (guardando.current) return
    guardando.current = true
    guardar.mutate(nivel, { onSettled: () => (guardando.current = false) })
  }

  return (
    <div className="flex flex-col gap-3">
      <MensajeAlma>
        <p>{PREGUNTA_ANIMO}</p>
      </MensajeAlma>
      {guardar.isError ? <Alert variant="error">{textoDeError(guardar.error)}</Alert> : null}
      <div role="group" aria-label={NOMBRE_GRUPO_ANIMO} className="grid grid-cols-5">
        {NIVELES_ANIMO.map((nivel) => (
          <CaritaAnimo key={nivel} nivel={nivel} deshabilitada={guardar.isPending} onElegir={elegir} />
        ))}
      </div>
    </div>
  )
}
