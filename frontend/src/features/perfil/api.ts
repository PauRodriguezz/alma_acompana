// Depende de GET /api/perfil del contrato (docs/contrato-login.md, sección 4.4).
import { api } from "@/lib/api"

export type Perfil = {
  nombre: string
  apellido: string
  /** yyyy-MM-dd, o null si la persona no la tiene cargada. */
  fechaNacimiento: string | null
  nombreUsuario: string
}

export function obtenerPerfil(): Promise<Perfil> {
  return api<Perfil>("/perfil")
}
