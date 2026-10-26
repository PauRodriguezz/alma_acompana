import type { PreferenciasAccesibilidad } from "./tipos"

/**
 * Aplica las preferencias en <html>: data-tamanio y data-contraste, que usa index.css
 * (contrato, sección 9). No guarda nada: para eso está almacen.ts.
 */
export function aplicarPreferencias(preferencias: PreferenciasAccesibilidad) {
  const html = document.documentElement
  html.dataset.tamanio = preferencias.tamanioLetra
  if (preferencias.altoContraste) {
    html.dataset.contraste = "alto"
  } else {
    delete html.dataset.contraste
  }
}
