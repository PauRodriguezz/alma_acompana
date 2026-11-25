import type { EtapaDeVida } from "./tipos"

/** Las cinco etapas del back y la parada "Otros momentos" (recuerdos sin etapa). */
export type IdEtapa = EtapaDeVida | "OTROS"

export type Etapa = {
  id: IdEtapa
  nombre: string
  rango: string
  /** Banderín, borde de la etapa elegida y texto fuerte: `var(--etapa-<id>-fuerte)` de `index.css` (contrato, Anexo A). */
  fuerte: string
  /** Fondo del círculo de la ilustración y borde de la tarjeta: `var(--etapa-<id>-claro)` de `index.css`. */
  claro: string
}

const colores = (id: string) => ({ fuerte: `var(--etapa-${id}-fuerte)`, claro: `var(--etapa-${id}-claro)` })

/** Orden del camino (contrato, 2.1). Los colores no están acá: salen de los tokens de `index.css`. */
export const ETAPAS: readonly Etapa[] = [
  { id: "INFANCIA", nombre: "Infancia", rango: "0 a 11 años", ...colores("infancia") },
  { id: "ADOLESCENCIA", nombre: "Adolescencia", rango: "12 a 17 años", ...colores("adolescencia") },
  { id: "JUVENTUD", nombre: "Juventud", rango: "18 a 29 años", ...colores("juventud") },
  { id: "ADULTEZ", nombre: "Adultez", rango: "30 a 59 años", ...colores("adultez") },
  { id: "VEJEZ", nombre: "Vejez", rango: "60 años o más", ...colores("vejez") },
  { id: "OTROS", nombre: "Otros momentos", rango: "Todavía sin etapa", ...colores("otros") },
]

export function etapaDe(id: IdEtapa): Etapa {
  return ETAPAS.find((e) => e.id === id) as Etapa
}

/** Los colores de la etapa como variables CSS: las clases usan `--etapa-fuerte` y `--etapa-claro`. */
export function variablesDeEtapa(etapa: Etapa): Record<string, string> {
  return { "--etapa-fuerte": etapa.fuerte, "--etapa-claro": etapa.claro }
}
