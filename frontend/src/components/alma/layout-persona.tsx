import { useRef } from "react"
import { Link, Outlet, useLocation } from "react-router-dom"

import { AvatarAlma } from "@/components/alma/avatar-alma"
import { BotonAccesibilidadMenu } from "@/components/alma/boton-accesibilidad-menu"
import { DESTINOS_PERSONA, RUTA_PERFIL } from "@/components/alma/destinos-menu"
import { MenuPrincipal } from "@/components/alma/menu-principal"
import { SaltarAlContenido } from "@/components/alma/saltar-al-contenido"
import { PanelAccesibilidadProvider } from "@/features/accesibilidad/PanelAccesibilidadProvider"
import { AvisoProvider } from "@/features/avisos/AvisoProvider"
import { NAVEGACION_ENLACE_ALMA, NAVEGACION_NOMBRE_ALMA, NAVEGACION_PRINCIPAL } from "@/lib/mensajes"

/**
 * Layout compartido de las pantallas de PERSONA_MAYOR (CU-13): Alma, la navegación, el contenido
 * y el botón de accesibilidad, que se montan una sola vez y en el mismo lugar en todas.
 * - Orden de Tab: "Saltar al contenido", el menú (MenuPrincipal) y después el contenido, igual que en el panel.
 * - Celular: Alma arriba a la izquierda y el botón de accesibilidad a la derecha (en el flujo, se desplazan con la
 *   pantalla), el contenido y la barra inferior de MenuPrincipal (solo íconos, de alto fijo --nav-inferior). Está
 *   `sticky` al pie y dentro del flujo, así que nunca tapa el final del contenido.
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
          <SaltarAlContenido />
          <MenuPrincipal
            rol="persona"
            destinos={DESTINOS_PERSONA}
            etiquetaNavegacion={NAVEGACION_PRINCIPAL}
            marca={{ nombre: NAVEGACION_NOMBRE_ALMA, enlaceA: "/" }}
            pie={<BotonAccesibilidadMenu ubicacion="menu" />}
          />

          <div className="flex-1 lg:pl-(--sidebar)">
            <div className="mt-(--page-top) flex h-[calc(44rem/17)] items-center justify-between px-(--page-x) lg:hidden">
              <Link to="/" aria-label={NAVEGACION_ENLACE_ALMA} className="inline-flex rounded-full">
                <AvatarAlma tamano={44} />
              </Link>
              <BotonAccesibilidadMenu ubicacion="barra" ocultoEnCelular={pathname === RUTA_PERFIL} />
            </div>
            <main
              ref={contenidoRef}
              id="contenido"
              tabIndex={-1}
              className="outline-none mx-auto w-full max-w-(--content-max) lg:mx-0 px-(--page-x) pt-4 pb-6 lg:pt-(--page-top)"
            >
              <Outlet />
            </main>
          </div>
        </div>
      </AvisoProvider>
    </PanelAccesibilidadProvider>
  )
}
