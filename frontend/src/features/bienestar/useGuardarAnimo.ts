import { useMutation } from "@tanstack/react-query"

import { modificarAnimo, registrarAnimo } from "./api"
import type { NivelAnimo, RegistroAnimo } from "./tipos"

type Opciones = {
  /** Sin valor se crea un registro (POST); con el id del registro recién creado se corrige ese (PUT). */
  registroId?: number
  /** Se llama una sola vez por cada guardado exitoso, con el registro y su pregunta contextual. */
  onRegistrado: (registro: RegistroAnimo) => void
}

/**
 * Guarda el ánimo que eligió la persona (7.1 y 7.2). Sin reintentos (el cliente de consultas ya los
 * desactiva para las mutaciones). No hay consultas ni caché de ánimo: no se invalida nada.
 */
export function useGuardarAnimo({ registroId, onRegistrado }: Opciones) {
  return useMutation({
    mutationFn: (nivel: NivelAnimo) =>
      registroId === undefined ? registrarAnimo(nivel) : modificarAnimo(registroId, nivel),
    onSuccess: onRegistrado,
  })
}
