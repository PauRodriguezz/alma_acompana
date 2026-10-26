// Según el DC: PreferenciaAccesibilidad (tamanio_letra, alto_contraste) y el enum TamanioLetra.
export type TamanioLetra = "NORMAL" | "GRANDE" | "MUY_GRANDE"

export type PreferenciasAccesibilidad = {
  tamanioLetra: TamanioLetra
  altoContraste: boolean
}

export const TAMANIOS_LETRA: readonly TamanioLetra[] = ["NORMAL", "GRANDE", "MUY_GRANDE"]

/** Valores por defecto de la aplicación (CU-13, camino alterno Nº1). */
export const PREFERENCIAS_POR_DEFECTO: PreferenciasAccesibilidad = {
  tamanioLetra: "NORMAL",
  altoContraste: false,
}
