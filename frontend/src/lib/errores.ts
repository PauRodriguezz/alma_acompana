import type { FieldValues, Path, UseFormSetError } from "react-hook-form"

import { ApiError } from "./api"
import {
  MENSAJE_ALGO_SALIO_MAL,
  MENSAJE_DATOS_INVALIDOS,
  MENSAJE_NO_ENCONTRADO,
  MENSAJE_SESION_VENCIDA,
  MENSAJE_SIN_CONEXION,
  MENSAJE_SIN_PERMISO,
} from "./mensajes"

/**
 * Texto a mostrar para un error de la API (contrato de recordatorios, sección 8).
 * status 0 = sin conexión, 401 = sesión terminada, 403 = sin permiso, 404 = no encontrado,
 * 5xx = algo salió mal. Cualquier otro 4xx usa el detalle de la API si lo trae. Para el 400 usa el texto que le pase la pantalla en `porEstado`
 * o, si no hay, el detalle de la API. `porEstado` también puede cambiar el de otro estado.
 */
export function mensajeDeError(
  error: unknown,
  porEstado: Partial<Record<number, string>> = {}
): string {
  if (!(error instanceof ApiError)) return MENSAJE_ALGO_SALIO_MAL
  const propio = porEstado[error.status]
  if (propio) return propio
  if (error.status === 0) return MENSAJE_SIN_CONEXION
  if (error.status === 401) return MENSAJE_SESION_VENCIDA
  if (error.status === 403) return MENSAJE_SIN_PERMISO
  if (error.status === 404) return MENSAJE_NO_ENCONTRADO
  if (error.status === 400) return error.detalle ?? MENSAJE_DATOS_INVALIDOS
  // Otro error del pedido (por ejemplo el 413 de una imagen pesada): el back manda el texto simple.
  if (error.status >= 400 && error.status < 500 && error.detalle) return error.detalle
  return MENSAJE_ALGO_SALIO_MAL
}

/**
 * Vuelca la lista `errores` de un 400 en los campos de react-hook-form, para que cada mensaje
 * aparezca junto a su campo. `alias` sirve cuando un campo de la API se muestra en otro del
 * formulario (por ejemplo `fechaHora` en el campo "fecha"). Los errores de campos que el
 * formulario no tiene (por ejemplo `fechaInicio`) no se pierden: se devuelven para mostrarlos
 * en el aviso de arriba.
 */
export function aplicarErroresDeCampos<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  camposDelFormulario: readonly string[],
  alias: Record<string, string> = {}
): string[] {
  if (!(error instanceof ApiError) || !error.errores) return []
  const sinCampo: string[] = []
  for (const { campo, mensaje } of error.errores) {
    const destino = alias[campo] ?? campo
    if (camposDelFormulario.includes(destino)) {
      setError(destino as Path<T>, { type: "server", message: mensaje })
    } else {
      sinCampo.push(mensaje)
    }
  }
  return sinCampo
}
