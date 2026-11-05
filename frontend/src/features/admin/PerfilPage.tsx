import { useEffect, type ReactNode } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { AccessibilityIcon, ArrowRight01Icon, Logout01Icon } from "@hugeicons/core-free-icons"

import { PanelAccesibilidad } from "@/components/alma/panel-accesibilidad"
import { Dialog, DialogTrigger } from "@/components/ui/dialog"
import { DialogoCerrarSesion } from "@/features/auth/DialogoCerrarSesion"
import { useAuth } from "@/features/auth/useAuth"
import {
  AJUSTE_ACCESIBILIDAD,
  SUBTITULO_PERFIL,
  TITULO_AJUSTES,
  TITULO_DATOS,
} from "@/features/perfil/mensajes"
import { CERRAR_SESION } from "@/lib/mensajes"

import { FILA, FILA_BOTON } from "./filas-perfil"
import {
  ADMIN_PERFIL,
  ADMIN_PERFIL_APELLIDO,
  ADMIN_PERFIL_NOMBRE,
  ADMIN_PERFIL_NOMBRE_USUARIO,
  ADMIN_PERFIL_ROL,
  ADMIN_ROL_ADMINISTRADOR,
  ADMIN_ROL_PERSONA_MAYOR,
  ADMIN_TITULO_PESTANIA,
} from "./mensajes"

const TEXTO_ROL = {
  ADMINISTRADOR: ADMIN_ROL_ADMINISTRADOR,
  PERSONA_MAYOR: ADMIN_ROL_PERSONA_MAYOR,
} as const

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <div className={`${FILA} items-baseline justify-between gap-4`}>
      <dt className="text-muted-foreground">{etiqueta}</dt>
      <dd className="text-right font-bold break-words">{children}</dd>
    </div>
  )
}

/**
 * Perfil de la persona administradora (contrato del panel, secciones 1 y 3.5).
 * PROPUESTO: no hay pantalla en el Figma; sigue el diseño del Perfil de la persona mayor
 * (encabezado con avatar, "Mis datos" y "Ajustes"), sin migas de pan. Usa los datos de la
 * sesión (GET /api/auth/me o el login), así que no hace otra llamada.
 * - "Tamaño de letra y contraste" abre el mismo panel que el botón flotante: un Dialog propio
 *   con PanelAccesibilidad, como BotonAccesibilidad. Las preferencias son un estado compartido,
 *   así que los dos muestran lo mismo; al cerrar, el foco vuelve a la fila.
 * - No usa EncabezadoPagina (su h1 sería "Perfil"): el título de la pestaña se pone acá.
 */
export function PerfilPage() {
  const { usuario } = useAuth()

  useEffect(() => {
    document.title = ADMIN_TITULO_PESTANIA(ADMIN_PERFIL)
  }, [])

  return (
    <div className="flex flex-col gap-6">
      <header className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h1 className="wrap-break-word lg:text-h1-desktop">
            {usuario?.nombre} {usuario?.apellido}
          </h1>
          <p className="text-muted-foreground">{SUBTITULO_PERFIL}</p>
        </div>
        <span
          aria-hidden="true"
          className="flex size-[calc(72rem/17)] shrink-0 items-center justify-center rounded-full bg-accent-soft font-heading text-h1 font-medium text-primary contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:text-foreground"
        >
          {usuario?.nombre.charAt(0).toUpperCase()}
        </span>
      </header>

      <div className="flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:items-start lg:gap-x-(--slot)">
        <section aria-labelledby="perfil-admin-datos">
          <h2 id="perfil-admin-datos" className="text-h3">
            {TITULO_DATOS}
          </h2>
          {usuario ? (
            <dl>
              <Dato etiqueta={ADMIN_PERFIL_NOMBRE}>{usuario.nombre}</Dato>
              <Dato etiqueta={ADMIN_PERFIL_APELLIDO}>{usuario.apellido}</Dato>
              <Dato etiqueta={ADMIN_PERFIL_NOMBRE_USUARIO}>{usuario.nombreUsuario}</Dato>
              <Dato etiqueta={ADMIN_PERFIL_ROL}>{TEXTO_ROL[usuario.rol]}</Dato>
            </dl>
          ) : null}
        </section>

        <section aria-labelledby="perfil-admin-ajustes">
          <h2 id="perfil-admin-ajustes" className="text-h3">
            {TITULO_AJUSTES}
          </h2>
          <ul>
            <li>
              <Dialog>
                <DialogTrigger aria-haspopup="dialog" className={FILA_BOTON}>
                  <HugeiconsIcon icon={AccessibilityIcon} className="size-6 shrink-0" aria-hidden="true" />
                  <span className="flex-1">{AJUSTE_ACCESIBILIDAD}</span>
                  <HugeiconsIcon icon={ArrowRight01Icon} className="size-6 shrink-0" aria-hidden="true" />
                </DialogTrigger>
                <PanelAccesibilidad />
              </Dialog>
            </li>
            <li>
              <DialogoCerrarSesion
                disparador={
                  <button type="button" className={`${FILA_BOTON} text-destructive`}>
                    <HugeiconsIcon icon={Logout01Icon} className="size-6 shrink-0" aria-hidden="true" />
                    <span className="flex-1">{CERRAR_SESION}</span>
                  </button>
                }
              />
            </li>
          </ul>
        </section>
      </div>
    </div>
  )
}
