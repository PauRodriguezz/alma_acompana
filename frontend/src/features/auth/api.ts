// Depende de POST /api/auth/login y GET /api/auth/me del contrato (docs/contrato-login.md).
import type { PreferenciasAccesibilidad } from "@/features/accesibilidad/tipos"
import { api } from "@/lib/api"

export type Rol = "PERSONA_MAYOR" | "ADMINISTRADOR"

export type Usuario = {
  id: number
  nombreUsuario: string
  nombre: string
  apellido: string
  rol: Rol
  /** Solo viene para PERSONA_MAYOR; para ADMINISTRADOR es null. */
  preferenciasAccesibilidad: PreferenciasAccesibilidad | null
}

export type LoginResponse = {
  token: string
  tipoToken: string
  expiraEnSegundos: number
  usuario: Usuario
}

export function login(
  nombreUsuario: string,
  contrasenia: string,
  mantenerSesion: boolean
): Promise<LoginResponse> {
  return api<LoginResponse>("/auth/login", {
    method: "POST",
    cuerpo: { nombreUsuario, contrasenia, mantenerSesion },
  })
}

export function me(): Promise<Usuario> {
  return api<Usuario>("/auth/me")
}
