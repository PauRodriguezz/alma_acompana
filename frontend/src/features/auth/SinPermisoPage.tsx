import { Link } from "react-router-dom"

import { buttonVariants } from "@/components/ui/button-variants"

import { rutaInicio } from "./rutas"
import { useAuth } from "./useAuth"

export function SinPermisoPage() {
  const { rol } = useAuth()

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-(--narrow-max) flex-col items-start gap-4 px-(--page-x) pt-(--page-top)">
      <h1>No tenés permiso para ver esta pantalla.</h1>
      <Link to={rol ? rutaInicio(rol) : "/login"} className={buttonVariants()}>
        Volver al inicio
      </Link>
    </main>
  )
}
