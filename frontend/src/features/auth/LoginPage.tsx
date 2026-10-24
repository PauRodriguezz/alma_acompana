import { useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Navigate, useLocation } from "react-router-dom"

import almaMarca from "@/assets/alma-marca.svg"
import almaSaludando from "@/assets/alma-saludando.png"
import { CampoTexto } from "@/components/alma/campo-texto"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Casilla } from "@/components/ui/casilla"
import { ApiError } from "@/lib/api"

import { rutaInicio } from "./rutas"
import { esquemaLogin, type DatosLogin } from "./schemas"
import { useAuth } from "./useAuth"

const TEXTO_CREDENCIALES = "Usuario o contraseña incorrectos. Intentá nuevamente."
// PROPUESTO: este texto no está en el contrato ni en los casos de uso.
const TEXTO_CONEXION = "No pudimos conectarnos. Probá de nuevo en un rato."

const ID_ERROR = "login-error"

export function LoginPage() {
  const { usuario, cargando, login } = useAuth()
  const location = useLocation()
  const aviso = (location.state as { aviso?: string } | null)?.aviso

  const [mostrarContrasenia, setMostrarContrasenia] = useState(false)
  const [falla, setFalla] = useState<"credenciales" | "conexion" | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({
    resolver: zodResolver(esquemaLogin),
    defaultValues: { nombreUsuario: "", contrasenia: "", mantenerSesion: true },
  })

  if (cargando) {
    return (
      <p role="status" className="p-(--page-x)">
        Cargando…
      </p>
    )
  }
  if (usuario) return <Navigate to={rutaInicio(usuario.rol)} replace />

  async function enviar(datos: DatosLogin) {
    setFalla(null)
    try {
      // Al entrar, usuario pasa a tener valor y esta pantalla redirige según el rol.
      await login(datos.nombreUsuario, datos.contrasenia, datos.mantenerSesion)
    } catch (error) {
      setFalla(error instanceof ApiError && error.status === 401 ? "credenciales" : "conexion")
    }
  }

  const describedBy = falla ? ID_ERROR : undefined

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-(--narrow-max) flex-col px-(--page-x) pt-13 pb-6 lg:items-center lg:justify-center lg:py-(--page-top)">
      <div className="flex w-full flex-col gap-5 lg:max-w-[calc(480rem/17)] lg:gap-4 lg:rounded-xl lg:bg-card lg:p-8 lg:shadow-sm">
        {/* Celular: Alma saludando, título y subtítulo, centrados */}
        <div className="flex flex-col items-center gap-2 text-center lg:hidden">
          <img
            src={almaSaludando}
            alt=""
            className="h-[calc(113rem/17)] w-[calc(94rem/17)] object-cover"
          />
          <h1>¡Hola! Soy Alma</h1>
          <p className="text-muted-foreground">Ingresá con tu usuario y tu contraseña.</p>
        </div>

        {/* Escritorio: marca a la izquierda dentro de la tarjeta */}
        <div className="hidden items-center gap-4 lg:flex">
          <img src={almaMarca} alt="" className="size-14 shrink-0" />
          <div className="flex flex-col gap-0.5">
            <h1 className="text-h2-desktop">Ingresá a Alma</h1>
            <p className="text-muted-foreground">Alma Acompaña</p>
          </div>
        </div>

        {aviso ? <Alert variant="atencion">{aviso}</Alert> : null}
        {falla ? (
          <Alert variant="error" id={ID_ERROR}>
            {falla === "credenciales" ? TEXTO_CREDENCIALES : TEXTO_CONEXION}
          </Alert>
        ) : null}

        <form
          onSubmit={handleSubmit(enviar)}
          noValidate
          className="flex flex-col gap-5 lg:gap-4"
        >
          <CampoTexto
            label="Usuario"
            type="text"
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            placeholder="Escribí tu usuario"
            error={errors.nombreUsuario?.message}
            invalido={falla === "credenciales"}
            aria-describedby={describedBy}
            {...register("nombreUsuario")}
          />
          <CampoTexto
            label="Contraseña"
            type={mostrarContrasenia ? "text" : "password"}
            autoComplete="current-password"
            placeholder="Escribí tu contraseña"
            error={errors.contrasenia?.message}
            invalido={falla === "credenciales"}
            aria-describedby={describedBy}
            {...register("contrasenia")}
          />

          <div className="flex flex-col">
            <Casilla
              label="Mostrar contraseña"
              checked={mostrarContrasenia}
              onChange={(evento) => setMostrarContrasenia(evento.target.checked)}
            />
            <Casilla label="Mantener sesión iniciada" {...register("mantenerSesion")} />
          </div>

          <Button type="submit" disabled={isSubmitting} className="w-full">
            {isSubmitting ? "Ingresando…" : "Ingresar"}
          </Button>
        </form>
      </div>
    </main>
  )
}
