import type { PreferenciasAccesibilidad } from "./api"

/**
 * Aplica las preferencias de accesibilidad de la sesión en <html> (contrato, sección 9):
 * data-tamanio y data-contraste, que usa index.css. Sin preferencias, vuelve a los valores
 * por defecto. No se guarda nada en el navegador.
 */
export function aplicarPreferencias(preferencias: PreferenciasAccesibilidad | null) {
  const html = document.documentElement
  html.dataset.tamanio = preferencias?.tamanioLetra ?? "NORMAL"
  if (preferencias?.altoContraste) {
    html.dataset.contraste = "alto"
  } else {
    delete html.dataset.contraste
  }
}
