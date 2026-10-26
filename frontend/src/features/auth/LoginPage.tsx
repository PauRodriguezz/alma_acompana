import { useState } from "react"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Navigate, useLocation } from "react-router-dom"

import alma from "@/assets/alma_acompana.webp"
import { BotonAccesibilidad } from "@/components/alma/boton-accesibilidad"
import { CampoTexto } from "@/components/alma/campo-texto"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Casilla } from "@/components/ui/casilla"
import { ApiError } from "@/lib/api"

import { rutaInicio } from "./rutas"
import { esquemaLogin, type DatosLogin } from "./schemas"
import { useAuth } from "./useAuth"

const TEXTO_CREDENCIALES = "Usuario o contraseña incorrectos. Intentá nuevamente."
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
    control,
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
    <main className="min-h-dvh w-full lg:flex lg:items-center lg:justify-center lg:px-(--page-x) lg:py-(--page-top)">
      {/* CU-13: botón flotante de accesibilidad. En celular queda sobre la banda de color. */}
      <BotonAccesibilidad />
      <div className="mx-auto flex min-h-dvh w-full flex-col lg:min-h-0 lg:max-w-(--content-max) lg:flex-row lg:rounded-xl lg:bg-card lg:[box-shadow:var(--sombra-tarjeta)] contraste-alto:lg:border-2 contraste-alto:lg:border-foreground">
        {/* Panel de color, con degradé. Sin overflow oculto: Alma se sale de él a propósito. */}
        <div className="relative h-[calc(190rem/17)] shrink-0 bg-[linear-gradient(160deg,var(--banda-arriba),var(--banda-abajo))] lg:h-auto lg:w-2/5 lg:rounded-l-xl contraste-alto:lg:rounded-l-[calc(22rem/17)] contraste-alto:lg:border-r-2 contraste-alto:lg:border-foreground">
          <img
            src={alma}
            alt=""
            className="pointer-events-none absolute bottom-[calc(24rem/17)] left-1/2 z-10 h-[calc(160rem/17)] w-auto max-w-none -translate-x-1/2 translate-y-1/2 object-contain [filter:var(--sombra-alma)] lg:top-1/2 lg:bottom-auto lg:left-[calc(100%_-_calc(56rem/17))] lg:h-[calc(360rem/17)] lg:max-h-[90%] lg:-translate-y-1/2"
          />
        </div>

        {/* Celular: hoja blanca con las esquinas de arriba redondeadas, sobre la banda.
            Escritorio: panel del formulario, sin fondo propio (lo da la tarjeta). */}
        <div className="relative -mt-[calc(24rem/17)] flex w-full flex-1 flex-col gap-5 rounded-t-xl bg-card px-(--page-x) pt-[calc(88rem/17)] pb-6 contraste-alto:border-t-2 contraste-alto:border-foreground lg:mt-0 lg:justify-center lg:gap-4 lg:rounded-none lg:bg-transparent lg:py-12 lg:pr-[calc(48rem/17)] lg:pl-[calc(96rem/17)] contraste-alto:lg:border-t-0">
        <div className="flex flex-col items-center gap-2 text-center lg:items-start lg:text-left">
          {/* Celular */}
          <div className="flex flex-col items-center gap-2 lg:hidden">
            <h1>¡Hola! Soy Alma</h1>
            <p className="text-muted-foreground">Ingresá con tu usuario y tu contraseña.</p>
          </div>
          {/* Escritorio */}
          <div className="hidden flex-col items-start gap-0.5 lg:flex">
            <h1 className="text-h2-desktop">¡Hola! Soy Alma</h1>
            <p className="text-muted-foreground">Ingresá con tu usuario y tu contraseña.</p>
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
              onCheckedChange={(marcada) => setMostrarContrasenia(marcada)}
            />
            {/* La cajita no es un <input> nativo: va con Controller (marcada por defecto) */}
            <Controller
              control={control}
              name="mantenerSesion"
              render={({ field }) => (
                <Casilla
                  label="Mantener sesión iniciada"
                  name={field.name}
                  ref={field.ref}
                  checked={field.value}
                  onCheckedChange={(marcada) => field.onChange(marcada)}
                  onBlur={field.onBlur}
                />
              )}
            />
          </div>

          <Button type="submit" disabled={isSubmitting} className="w-full">
            {isSubmitting ? "Ingresando…" : "Ingresar"}
          </Button>
        </form>
        </div>
      </div>
    </main>
  )
}
