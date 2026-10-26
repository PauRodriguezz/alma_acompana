import { TAMANIOS_LETRA, type PreferenciasAccesibilidad } from "./tipos"

const CLAVE = "alma.preferencias"

// Si el navegador no deja usar storage (modo privado, datos bloqueados), siguen en memoria.
let enMemoria: PreferenciasAccesibilidad | null = null

function esValida(valor: unknown): valor is PreferenciasAccesibilidad {
  if (typeof valor !== "object" || valor === null) return false
  const { tamanioLetra, altoContraste } = valor as Record<string, unknown>
  return (
    typeof altoContraste === "boolean" &&
    TAMANIOS_LETRA.includes(tamanioLetra as PreferenciasAccesibilidad["tamanioLetra"])
  )
}

/** Última configuración usada en este dispositivo, o null si nunca se guardó (o está dañada). */
export function leerPreferenciasDispositivo(): PreferenciasAccesibilidad | null {
  try {
    const texto = localStorage.getItem(CLAVE)
    if (texto) {
      const valor: unknown = JSON.parse(texto)
      if (esValida(valor)) return valor
    }
  } catch {
    // storage bloqueado o JSON dañado: se usa la copia en memoria
  }
  return enMemoria
}

export function guardarPreferenciasDispositivo(preferencias: PreferenciasAccesibilidad): void {
  enMemoria = preferencias
  try {
    localStorage.setItem(CLAVE, JSON.stringify(preferencias))
  } catch {
    // sigue en memoria
  }
}
