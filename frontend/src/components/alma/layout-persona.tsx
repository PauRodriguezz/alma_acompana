import { useRef } from "react"
import { Link, Outlet, useLocation } from "react-router-dom"

import alma from "@/assets/alma_acompana.webp"
import { BotonAccesibilidadMenu } from "@/components/alma/boton-accesibilidad-menu"
import { NavegacionPrincipal } from "@/components/alma/navegacion-principal"
import { RUTA_PERFIL } from "@/components/alma/rutas-persona"
import { PanelAccesibilidadProvider } from "@/features/accesibilidad/PanelAccesibilidadProvider"
import { AvisoProvider } from "@/features/avisos/AvisoProvider"
import { NAVEGACION_ENLACE_ALMA, NAVEGACION_NOMBRE_ALMA } from "@/lib/mensajes"

/**
 * Layout compartido de las pantallas de PERSONA_MAYOR (CU-13): Alma, la navegación, el contenido
 * y el botón de accesibilidad, que se montan una sola vez y en el mismo lugar en todas.
 * - Celular: Alma arriba a la izquierda (con su alto reservado, para no pisar los títulos), el
 *   contenido y la barra inferior. La barra es `sticky` al pie, de alto fijo (--nav-inferior) y
 *   está dentro del flujo, así que nunca tapa el final del contenido.
 * - Escritorio (1024 px o más): menú lateral fijo de ancho --sidebar y el contenido a su derecha.
 * - Aviso flotante (AvisoProvider): vive acá para sobrevivir al cambio de pantalla.
 */
export function LayoutPersona() {
  const { pathname } = useLocation()
  const contenidoRef = useRef<HTMLElement>(null)

  return (
    <PanelAccesibilidadProvider>
      <AvisoProvider contenidoRef={contenidoRef}>
      <div className="flex min-h-dvh flex-col lg:block">
        <div className="flex-1 lg:pl-(--sidebar)">
          <div className="mt-(--page-top) flex h-[calc(44rem/17)] items-center px-(--page-x) lg:hidden">
            <Link
              to="/"
              aria-label={NAVEGACION_ENLACE_ALMA}
              className="inline-flex size-[calc(44rem/17)] items-center justify-center rounded-full bg-accent-soft contraste-alto:border-2 contraste-alto:border-foreground"
            >
              <img src={alma} alt="" className="size-[calc(36rem/17)] object-contain" />
            </Link>
          </div>
          <main
            ref={contenidoRef}
            tabIndex={-1}
            className="outline-none mx-auto w-full max-w-(--content-max) lg:mx-0 px-(--page-x) pt-4 pb-6 lg:pt-(--page-top)"
          >
            <Outlet />
          </main>
        </div>

        <div className="sticky bottom-0 z-30 h-[calc(var(--nav-inferior)+env(safe-area-inset-bottom))] border-t-2 border-border bg-card pb-[env(safe-area-inset-bottom)] lg:h-auto lg:fixed lg:inset-y-0 lg:left-0 lg:flex lg:w-(--sidebar) lg:flex-col lg:overflow-y-auto lg:border-t-0 lg:border-r-2 lg:pb-0">
          <Link
            to="/"
            className="hidden items-center gap-3 px-4 py-6 font-heading text-h3 font-medium text-heading lg:flex"
          >
            <span className="inline-flex size-[calc(40rem/17)] items-center justify-center rounded-full bg-accent-soft contraste-alto:border-2 contraste-alto:border-foreground">
              <img src={alma} alt="" className="size-[calc(32rem/17)] object-contain" />
            </span>
            {NAVEGACION_NOMBRE_ALMA}
          </Link>
          <div className="px-2 py-1 lg:px-4 lg:py-0">
            <NavegacionPrincipal />
          </div>
          <BotonAccesibilidadMenu ocultoEnCelular={pathname === RUTA_PERFIL} />
        </div>
      </div>
      </AvisoProvider>
    </PanelAccesibilidadProvider>
  )
}
