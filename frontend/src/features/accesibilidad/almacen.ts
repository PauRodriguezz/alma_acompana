import { guardarPreferenciasCuenta } from "./api"
import { aplicarPreferencias } from "./aplicar"
import { guardarPreferenciasDispositivo, leerPreferenciasDispositivo } from "./dispositivo"
import { PREFERENCIAS_POR_DEFECTO, type PreferenciasAccesibilidad } from "./tipos"

// Estado de accesibilidad de toda la app. Lo que se usa es lo último elegido en el
// dispositivo (o la configuración de la cuenta al ingresar): cerrar o vencer la sesión no
// lo cambia.
let actuales: PreferenciasAccesibilidad =
  leerPreferenciasDispositivo() ?? PREFERENCIAS_POR_DEFECTO

// ¿La persona tocó las opciones durante esta visita? Decide, al iniciar sesión, si se
// mantiene lo que eligió o se usa lo que trae la cuenta.
let tocadas = false

const oyentes = new Set<() => void>()

export function obtenerPreferencias(): PreferenciasAccesibilidad {
  return actuales
}

export function suscribirse(oyente: () => void): () => void {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}

/** Aplica las preferencias a la interfaz y las guarda en el dispositivo. */
export function usarPreferencias(preferencias: PreferenciasAccesibilidad): void {
  actuales = preferencias
  aplicarPreferencias(preferencias)
  guardarPreferenciasDispositivo(preferencias)
  oyentes.forEach((oyente) => oyente())
}

/** Se llama al arrancar la app, antes de renderizar, para evitar un parpadeo con otro tamaño. */
export function iniciarAccesibilidad(): void {
  aplicarPreferencias(actuales)
}

export function marcarTocadas(valor: boolean): void {
  tocadas = valor
}

export function hubieronCambios(): boolean {
  return tocadas
}

// Los envíos van de a uno y en orden, para que un cambio viejo no pise a uno nuevo.
let cola: Promise<void> = Promise.resolve()

/**
 * Manda las preferencias a la cuenta (PUT /api/accesibilidad). Si falla no avisa ni corta
 * nada: la preferencia ya está aplicada y guardada en el dispositivo.
 * Solo para PERSONA_MAYOR con sesión.
 */
export function enviarACuenta(preferencias: PreferenciasAccesibilidad): void {
  cola = cola
    .then(() => guardarPreferenciasCuenta(preferencias))
    .then(
      () => undefined,
      () => undefined
    )
}
