import type { EtapaDeVida } from "./tipos"

/** Las cinco etapas del back y la parada "Otros momentos" (recuerdos sin etapa). */
export type IdEtapa = EtapaDeVida | "OTROS"

export type Etapa = {
  id: IdEtapa
  nombre: string
  rango: string
  /** Banderín, borde de la etapa elegida y texto fuerte (contrato, Anexo A). Contraste con blanco: 6,2 a 7,0. */
  fuerte: string
  /** Fondo del círculo de la ilustración y borde de la tarjeta. */
  claro: string
}

/** Orden del camino (contrato, 2.1). */
export const ETAPAS: readonly Etapa[] = [
  { id: "INFANCIA", nombre: "Infancia", rango: "0 a 11 años", fuerte: "#993C1D", claro: "#F5C4B3" },
  { id: "ADOLESCENCIA", nombre: "Adolescencia", rango: "12 a 17 años", fuerte: "#534AB7", claro: "#CECBF6" },
  { id: "JUVENTUD", nombre: "Juventud", rango: "18 a 29 años", fuerte: "#185FA5", claro: "#B5D4F4" },
  { id: "ADULTEZ", nombre: "Adultez", rango: "30 a 59 años", fuerte: "#0F6E56", claro: "#9FE1CB" },
  { id: "VEJEZ", nombre: "Vejez", rango: "60 años o más", fuerte: "#854F0B", claro: "#FAC775" },
  { id: "OTROS", nombre: "Otros momentos", rango: "Todavía sin etapa", fuerte: "#5F5E5A", claro: "#D3D1C7" },
]

export function etapaDe(id: IdEtapa): Etapa {
  return ETAPAS.find((e) => e.id === id) as Etapa
}

/** Los colores de la etapa como variables CSS: las clases usan `--etapa-fuerte` y `--etapa-claro`. */
export function variablesDeEtapa(etapa: Etapa): Record<string, string> {
  return { "--etapa-fuerte": etapa.fuerte, "--etapa-claro": etapa.claro }
}
