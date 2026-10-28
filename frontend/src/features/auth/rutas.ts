import type { Rol } from "./api"

/** Pantalla de inicio de cada rol: PERSONA_MAYOR en /, ADMINISTRADOR en /admin. */
export function rutaInicio(rol: Rol): string {
  return rol === "ADMINISTRADOR" ? "/admin" : "/"
}
