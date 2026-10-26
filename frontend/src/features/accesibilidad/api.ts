// Depende de PUT /api/accesibilidad del contrato (docs/contrato-login.md, sección 4.3).
// Solo PERSONA_MAYOR con sesión: un ADMINISTRADOR recibe 403, así que nunca se llama con ese rol.
// GET /api/accesibilidad no se usa: el login y /api/auth/me ya traen las preferencias.
import { api } from "@/lib/api"

import type { PreferenciasAccesibilidad } from "./tipos"

export function guardarPreferenciasCuenta(
  preferencias: PreferenciasAccesibilidad
): Promise<PreferenciasAccesibilidad> {
  return api<PreferenciasAccesibilidad>("/accesibilidad", {
    method: "PUT",
    cuerpo: preferencias,
  })
}
