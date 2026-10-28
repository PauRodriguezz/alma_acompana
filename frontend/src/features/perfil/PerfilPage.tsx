import type { ReactNode } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  AccessibilityIcon,
  ArrowRight01Icon,
  Logout01Icon,
} from "@hugeicons/core-free-icons"

import { Alert } from "@/components/ui/alert"
import { usePanelAccesibilidad } from "@/features/accesibilidad/usePanelAccesibilidad"
import { DialogoCerrarSesion } from "@/features/auth/DialogoCerrarSesion"
import { useAuth } from "@/features/auth/useAuth"

import { fechaCorta } from "@/lib/fechas"

import {
  AJUSTE_ACCESIBILIDAD,
  AJUSTE_CERRAR_SESION,
  CARGANDO_DATOS,
  ERROR_CARGA,
  ETIQUETA_APELLIDO,
  ETIQUETA_FECHA_NACIMIENTO,
  ETIQUETA_NOMBRE,
  ETIQUETA_USUARIO,
  FECHA_SIN_CARGAR,
  SUBTITULO_PERFIL,
  TITULO_AJUSTES,
  TITULO_DATOS,
} from "./mensajes"
import { usePerfil } from "./usePerfil"

const FILA = "flex w-full items-center gap-3 border-b-2 border-border py-3"
const FILA_BOTON = `${FILA} min-h-[calc(56rem/17)] text-left font-semibold transition-colors hover:bg-secondary`

function Dato({ etiqueta, children }: { etiqueta: string; children: ReactNode }) {
  return (
    <div className={`${FILA} items-baseline justify-between gap-4`}>
      <dt className="text-muted-foreground">{etiqueta}</dt>
      <dd className="text-right font-bold break-words">{children}</dd>
    </div>
  )
}

/** Perfil de la persona mayor (CU-13 y CU-14): sus datos, el panel de accesibilidad y cerrar sesión. */
export function PerfilPage() {
  const { usuario } = useAuth()
  const { abrir } = usePanelAccesibilidad()
  const estado = usePerfil()

  return (
    <div className="flex flex-col gap-6">
      {/* El nombre sale de la sesión: se ve de entrada, sin esperar a /api/perfil */}
      <header className="flex items-start justify-between gap-4">
        <div>
          <h1>
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
        <section aria-labelledby="perfil-datos">
          <h2 id="perfil-datos" className="text-h3">
            {TITULO_DATOS}
          </h2>
          {estado.tipo === "cargando" ? (
            <p role="status" className="py-3 text-muted-foreground">
              {CARGANDO_DATOS}
            </p>
          ) : null}
          {estado.tipo === "error" ? (
            <Alert variant="error" className="mt-3">
              {ERROR_CARGA}
            </Alert>
          ) : null}
          {estado.tipo === "listo" ? (
            <dl>
              <Dato etiqueta={ETIQUETA_NOMBRE}>{estado.perfil.nombre}</Dato>
              <Dato etiqueta={ETIQUETA_APELLIDO}>{estado.perfil.apellido}</Dato>
              <Dato etiqueta={ETIQUETA_FECHA_NACIMIENTO}>
                {estado.perfil.fechaNacimiento
                  ? fechaCorta(estado.perfil.fechaNacimiento)
                  : FECHA_SIN_CARGAR}
              </Dato>
              <Dato etiqueta={ETIQUETA_USUARIO}>{estado.perfil.nombreUsuario}</Dato>
            </dl>
          ) : null}
        </section>

        <section aria-labelledby="perfil-ajustes">
          <h2 id="perfil-ajustes" className="text-h3">
            {TITULO_AJUSTES}
          </h2>
          <ul>
            <li>
              <button type="button" aria-haspopup="dialog" onClick={abrir} className={FILA_BOTON}>
                <HugeiconsIcon icon={AccessibilityIcon} className="size-6 shrink-0" aria-hidden="true" />
                <span className="flex-1">{AJUSTE_ACCESIBILIDAD}</span>
                <HugeiconsIcon icon={ArrowRight01Icon} className="size-6 shrink-0" aria-hidden="true" />
              </button>
            </li>
            <li>
              <DialogoCerrarSesion>
                <button type="button" className={`${FILA_BOTON} text-destructive`}>
                  <HugeiconsIcon icon={Logout01Icon} className="size-6 shrink-0" aria-hidden="true" />
                  <span className="flex-1">{AJUSTE_CERRAR_SESION}</span>
                </button>
              </DialogoCerrarSesion>
            </li>
          </ul>
        </section>
      </div>
    </div>
  )
}
