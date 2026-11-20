import { useEffect, useMemo, useRef } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Outlet } from "react-router-dom"

import { BotonAccesibilidad } from "@/components/alma/boton-accesibilidad"
import { destinosOperador, ICONO_CERRAR_SESION } from "@/components/alma/destinos-menu"
import { ICONO_ITEM_MENU, ITEM_MENU } from "@/components/alma/menu-estilos"
import { MenuPrincipal } from "@/components/alma/menu-principal"
import { SaltarAlContenido } from "@/components/alma/saltar-al-contenido"
import { DialogoCerrarSesion } from "@/features/auth/DialogoCerrarSesion"
import { AvisoProvider } from "@/features/avisos/AvisoProvider"
import { CERRAR_SESION } from "@/lib/mensajes"

import {
  ADMIN_CERRAR_MENU,
  ADMIN_EJERCICIOS,
  ADMIN_MARCA,
  ADMIN_MENU,
  ADMIN_MENU_NAVEGACION,
  ADMIN_MENU_PANEL,
  ADMIN_MICROCAPSULAS,
  ADMIN_PANEL,
  ADMIN_PERFIL,
  ADMIN_TITULO_APP,
} from "./mensajes"

// Título de la pestaña fuera del panel (el de index.html)
const TITULO_APP = ADMIN_TITULO_APP

/** "Cerrar sesión" al pie del menú: abre el diálogo de confirmación. Mismo ítem que los destinos. */
function ItemCerrarSesion() {
  return (
    <DialogoCerrarSesion
      disparador={
        <button type="button" className={`${ITEM_MENU} cursor-pointer`}>
          <HugeiconsIcon icon={ICONO_CERRAR_SESION} className={ICONO_ITEM_MENU} aria-hidden="true" />
          <span>{CERRAR_SESION}</span>
        </button>
      }
    />
  )
}

/**
 * Layout del panel de operador (contrato del panel, sección 6). El menú es MenuPrincipal, el mismo componente
 * que usa la persona mayor, con los destinos del operador:
 * - Escritorio (desde 1024 px): idéntico al de la persona, salvo el fondo beige (Figma 51:1409) y el subtítulo
 *   "Panel de operador" debajo de "Alma".
 * - Celular: barra superior con el botón "Menú" (con texto), que abre una hoja lateral con los mismos destinos. En la
 *   barra no va la marca ("Alma / Panel de operador"): con letra grande dejaba demasiado texto arriba.
 * - PROPUESTO: el botón de accesibilidad (CU-13) también está en el panel, flotante; para el ADMINISTRADOR las
 *   preferencias quedan solo en el dispositivo (ver useAccesibilidad).
 */
export function AdminLayout() {
  const contenidoRef = useRef<HTMLElement>(null)
  const destinos = useMemo(
    () =>
      destinosOperador({
        panel: ADMIN_MENU_PANEL,
        ejercicios: ADMIN_EJERCICIOS,
        microcapsulas: ADMIN_MICROCAPSULAS,
        perfil: ADMIN_PERFIL,
      }),
    []
  )

  // Al salir del panel la pestaña vuelve a su título. Los datos guardados se borran en el
  // logout de AuthProvider.
  useEffect(
    () => () => {
      document.title = TITULO_APP
    },
    []
  )

  return (
    <AvisoProvider contenidoRef={contenidoRef} posicion="panel">
      <div className="min-h-dvh lg:pl-(--sidebar)">
        <SaltarAlContenido />
        <BotonAccesibilidad />
        <MenuPrincipal
          rol="operador"
          destinos={destinos}
          etiquetaNavegacion={ADMIN_MENU_NAVEGACION}
          marca={{ nombre: ADMIN_MARCA, subtitulo: ADMIN_PANEL }}
          pie={<ItemCerrarSesion />}
          textosCelularOperador={{ menu: ADMIN_MENU, cerrarMenu: ADMIN_CERRAR_MENU }}
        />

        <main
          ref={contenidoRef}
          id="contenido"
          tabIndex={-1}
          className="min-w-0 px-(--page-x) pt-(--page-top) pb-24 outline-none"
        >
          {/* @container: las pantallas se acomodan al ancho del contenido medido en rem, que
              crece con el tamaño de letra (los cortes lg:/xl: miran la ventana en px). */}
          <div className="@container mx-auto lg:mx-0 flex w-full max-w-(--content-max) flex-col gap-6">
            <Outlet />
          </div>
        </main>
      </div>
    </AvisoProvider>
  )
}
