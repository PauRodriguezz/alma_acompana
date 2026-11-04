// Depende de /api/animo del contrato (docs/contrato-animo.md, sección 7).
import { api } from "@/lib/api"

import type { NivelAnimo, RegistroAnimo } from "./tipos"

const BASE = "/animo"

/** 7.1: crea un registro nuevo. La fecha y la hora las pone el back. */
export function registrarAnimo(nivel: NivelAnimo): Promise<RegistroAnimo> {
  return api<RegistroAnimo>(BASE, { method: "POST", cuerpo: { nivelAnimo: nivel } })
}

/** 7.2: corrige el ánimo de hoy (cambia el nivel y la hora, y se mantiene el id). */
export function modificarAnimo(id: number, nivel: NivelAnimo): Promise<RegistroAnimo> {
  return api<RegistroAnimo>(`${BASE}/${id}`, { method: "PUT", cuerpo: { nivelAnimo: nivel } })
}
