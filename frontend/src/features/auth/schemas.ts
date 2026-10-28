import { z } from "zod"

export const esquemaLogin = z.object({
  nombreUsuario: z.string().trim().min(1, "Ingresá tu nombre de usuario."),
  contrasenia: z.string().min(1, "Ingresá tu contraseña."),
  mantenerSesion: z.boolean(),
})

export type DatosLogin = z.infer<typeof esquemaLogin>
