import { useEffect, useState } from "react"
import { Outlet } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { HugeiconsIcon } from "@hugeicons/react"
import { Menu01Icon } from "@hugeicons/core-free-icons"

import marcaAlma from "@/assets/alma-marca.svg"
import { BotonAccesibilidad } from "@/components/alma/boton-accesibilidad"
import { tamanioTextoBoton } from "@/components/alma/estilos"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"

import { MenuAdmin } from "./MenuAdmin"

// Título de la pestaña fuera del panel (el de index.html)
const TITULO_APP = "Alma Acompaña"

/**
 * Marca del Figma (40:316): avatar de Alma, "Alma" en h2-desktop y "Panel de operador".
 * compacta: en la barra de celular el avatar se oculta en pantallas muy angostas, para que
 * con letra grande entren "Menú", la marca y el botón de accesibilidad.
 */
function Marca({ compacta = false }: { compacta?: boolean }) {
  return (
    <div className="flex min-w-0 items-center gap-3">
      <img
        src={marcaAlma}
        alt=""
        className={`size-12 shrink-0 ${compacta ? "hidden sm:block" : ""}`}
      />
      <div className="flex min-w-0 flex-col">
        <span className="font-heading text-h2-desktop font-medium text-heading">Alma</span>
        <span className="text-caption text-muted-foreground">Panel de operador</span>
      </div>
    </div>
  )
}

/**
 * Layout del panel de operador (contrato del panel, sección 6).
 * - Escritorio (desde 1024 px): menú lateral fijo de ancho --sidebar, como el Figma (51:1409).
 * - Celular (PROPUESTO): barra superior con la marca y un botón "Menú" que abre el mismo
 *   menú en un panel lateral.
 * - PROPUESTO: el botón de accesibilidad (CU-13) también está en el panel; para el
 *   ADMINISTRADOR las preferencias quedan solo en el dispositivo (ver useAccesibilidad).
 */
export function AdminLayout() {
  const [menuAbierto, setMenuAbierto] = useState(false)
  const clienteConsultas = useQueryClient()

  // Al salir del panel (cerrar sesión o sesión vencida) se borran los datos guardados, así
  // la próxima cuenta que entre no ve datos de la anterior, y la pestaña vuelve a su título.
  useEffect(
    () => () => {
      clienteConsultas.clear()
      document.title = TITULO_APP
    },
    [clienteConsultas]
  )

  return (
    <div className="min-h-dvh lg:flex">
      <a
        href="#contenido"
        className="sr-only z-50 rounded-md bg-card px-4 py-3 font-semibold text-heading focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
      >
        Saltar al contenido
      </a>

      <BotonAccesibilidad />

      {/* Escritorio: menú lateral */}
      <aside className="sticky top-0 hidden h-dvh w-(--sidebar) shrink-0 flex-col gap-2 overflow-y-auto border-r-2 border-border bg-secondary px-4 py-6 lg:flex">
        <div className="px-2 pb-6">
          <Marca />
        </div>
        <MenuAdmin />
      </aside>

      {/* Celular: barra superior. A la derecha queda lugar para el botón de accesibilidad. */}
      <header className="flex items-center gap-3 border-b-2 border-border bg-secondary py-3 pr-[calc(var(--page-x)+calc(56rem/17))] pl-(--page-x) lg:hidden">
        <Sheet open={menuAbierto} onOpenChange={setMenuAbierto}>
          <SheetTrigger render={<Button variant="outline" className={`mb-0 ${tamanioTextoBoton}`} />}>
            <HugeiconsIcon icon={Menu01Icon} className="size-6" aria-hidden="true" />
            Menú
          </SheetTrigger>
          <SheetContent side="left" textoCerrar="Cerrar menú" className="px-4 pb-6">
            <SheetHeader className="px-2">
              <SheetTitle>Menú</SheetTitle>
            </SheetHeader>
            <MenuAdmin alNavegar={() => setMenuAbierto(false)} />
          </SheetContent>
        </Sheet>
        <Marca compacta />
      </header>

      <main
        id="contenido"
        tabIndex={-1}
        className="min-w-0 flex-1 px-(--page-x) pt-(--page-top) pb-24 outline-none"
      >
        {/* @container: las pantallas se acomodan al ancho del contenido medido en rem, que
            crece con el tamaño de letra (los cortes lg:/xl: miran la ventana en px). */}
        <div className="@container mx-auto flex w-full max-w-(--content-max) flex-col gap-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
