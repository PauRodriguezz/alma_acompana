import { ETAPAS, type Etapa } from "./etapas"
import type { RecuerdoResumen } from "./tipos"

export type EtapaConRecuerdos = { etapa: Etapa; recuerdos: RecuerdoResumen[] }

/**
 * Reparte los recuerdos por `etapaDeVida` (contrato, 2.1 y 2.2). Conserva el orden del back dentro de cada
 * etapa. Las cinco etapas salen siempre; "Otros momentos" solo si hay recuerdos sin etapa.
 */
export function agruparPorEtapa(recuerdos: RecuerdoResumen[]): EtapaConRecuerdos[] {
  const grupos = ETAPAS.map((etapa) => ({
    etapa,
    recuerdos: recuerdos.filter((r) => (r.etapaDeVida ?? "OTROS") === etapa.id),
  }))
  return grupos.filter((g) => g.etapa.id !== "OTROS" || g.recuerdos.length > 0)
}

/** Qué muestra la tarjeta de la parada (contrato, 2.1): hasta dos títulos y, con 3 o más, cuántos más hay. */
export function resumenDeTarjeta(recuerdos: RecuerdoResumen[]): { titulos: string[]; masDe: number } {
  if (recuerdos.length <= 2) return { titulos: recuerdos.map((r) => r.titulo), masDe: 0 }
  return { titulos: [recuerdos[0].titulo], masDe: recuerdos.length - 1 }
}
