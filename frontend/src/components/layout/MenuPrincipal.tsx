import { useState, type ComponentProps, type ReactNode } from "react"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { Link, NavLink } from "react-router-dom"

import { AvatarAlma } from "@/components/alma/Alma"
import { ICONO_MENU, type DestinoMenu } from "@/components/layout/destinosMenu"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"


// Estilo del ítem del menú lateral: es UNO solo para la persona y para el operador (los dos menús son
// idénticos en escritorio). Los valores son los del menú de la persona (Figma 40:273).
//
// - Ítem: 48 px de alto, ícono de 24 px (size-6), texto semibold, 12 px entre ícono y texto.
// - Activo: `aria-current="page"` (lo pone NavLink) con fondo accent-soft, barra de 4×24 px a la
//   izquierda y el ícono en accent. En alto contraste: borde de 2 px en foreground y fondo card. Todos
//   los ítems llevan borde transparente de 2 px solo en alto contraste, para que no cambien de tamaño.
const ITEM_MENU =
  "group relative flex min-h-[calc(48rem/17)] w-full items-center gap-3 rounded-md px-4 py-2 text-left font-semibold text-foreground transition-colors hover:bg-secondary-hover " +
  "before:absolute before:top-1/2 before:left-0 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-accent before:opacity-0 " +
  "aria-[current=page]:bg-accent-soft aria-[current=page]:before:opacity-100 aria-[current=page]:hover:bg-accent-soft " +
  "contraste-alto:border-2 contraste-alto:border-transparent contraste-alto:aria-[current=page]:border-foreground contraste-alto:aria-[current=page]:bg-card"

const ICONO_ITEM_MENU =
  "size-6 shrink-0 group-aria-[current=page]:text-accent contraste-alto:group-aria-[current=page]:text-foreground"

// Barra inferior del celular de la persona: SOLO ÍCONOS (el nombre queda para el lector de pantalla). Es una
// excepción documentada a "un ícono nunca va solo": los cinco nombres no entran en 1/5 del ancho de 360 px
// (menos con letra grande) y el Figma la dibuja así. A cambio: nombre para el lector de pantalla (sr-only),
// aria-current en el activo, objetivo táctil de 56 px de alto y 1/5 del ancho, y el activo distinguido por
// más que el color (fondo, barra arriba y, en alto contraste, borde de 2 px). El alto es fijo (--nav-inferior).
const ITEM_BARRA =
  "group relative flex min-h-[calc(56rem/17)] flex-col items-center justify-center gap-1 rounded-md px-1 py-2 text-foreground transition-colors hover:bg-secondary " +
  "before:absolute before:top-0 before:left-1/2 before:h-1 before:w-6 before:-translate-x-1/2 before:rounded-full before:bg-accent before:opacity-0 " +
  "aria-[current=page]:bg-accent-soft aria-[current=page]:before:opacity-100 " +
  "contraste-alto:border-2 contraste-alto:border-transparent contraste-alto:aria-[current=page]:border-foreground contraste-alto:aria-[current=page]:bg-card"

/** Un ítem del menú que es un botón y no un enlace ("Cerrar sesión" del operador): se ve igual que los destinos. */
export function BotonItemMenu({ icono, children, ...props }: ComponentProps<"button"> & { icono: IconSvgElement }) {
  return (
    <button type="button" {...props} className={`${ITEM_MENU} cursor-pointer`}>
      <HugeiconsIcon icon={icono} className={ICONO_ITEM_MENU} aria-hidden="true" />
      <span>{children}</span>
    </button>
  )
}

type Rol = "persona" | "operador"

type MenuPrincipalProps = {
  /** Decide el patrón de celular: la persona tiene la barra inferior; el operador, el botón "Menú" con su hoja. */
  rol: Rol
  /** Destinos del rol (components/layout/destinosMenu.ts): ruta, nombre e ícono. Este componente no elige íconos. */
  destinos: readonly DestinoMenu[]
  /** Nombre accesible de la navegación ("Principal", "Menú del panel"). */
  etiquetaNavegacion: string
  marca: {
    nombre: string
    /** Debajo del nombre (solo el operador: "Panel de operador"). */
    subtitulo?: string
    /** Si hay ruta, la marca es un enlace (la persona vuelve al Chat). */
    enlaceA?: string
  }
  /** Pie del menú de escritorio y de la hoja del celular: "Accesibilidad" (persona) o "Cerrar sesión" (operador). */
  pie: ReactNode
  /** Solo para el operador: textos del botón "Menú" y de la hoja. */
  textosCelularOperador?: { menu: string; cerrarMenu: string }
}

// El fondo es la única diferencia visual entre los dos menús (Figma: Rol=Usuario blanco, Rol=Operador beige).
const FONDO: Record<Rol, string> = {
  persona: "bg-card",
  operador: "bg-secondary",
}

/**
 * Menú de las dos áreas de la aplicación (persona mayor y operador). En escritorio (1024 px o más) los dos son
 * IDÉNTICOS: barra lateral fija, marca (avatar de 40 px y "Alma"), destinos con el mismo ítem (ver
 * ITEM_MENU) y pie. Solo cambian el fondo, el subtítulo de la marca, los destinos y el pie.
 * En celular:
 * - persona: barra inferior de SOLO ÍCONOS, de alto fijo (--nav-inferior, que usan el campo del chat y los avisos).
 *   Es una excepción documentada a "un ícono nunca va solo" (ver MenuPrincipal.tsx): cada destino tiene su nombre
 *   para el lector de pantalla, aria-current en el activo y un objetivo táctil de al menos 44 px.
 * - operador: botón "Menú" (con texto) en una barra superior que abre una hoja lateral con los mismos destinos.
 * Va ANTES del contenido en el DOM, así el orden del teclado es el mismo en los dos (después de "Saltar al contenido");
 * la barra inferior se ordena al pie de la pantalla con `order-last`.
 */
export function MenuPrincipal({
  rol,
  destinos,
  etiquetaNavegacion,
  marca,
  pie,
  textosCelularOperador,
}: MenuPrincipalProps) {
  const [hojaAbierta, setHojaAbierta] = useState(false)

  return (
    <>
      <aside
        className={`fixed inset-y-0 left-0 z-30 hidden w-(--sidebar) flex-col overflow-y-auto border-r-2 border-border lg:flex ${FONDO[rol]}`}
      >
        <Marca {...marca} />
        <div className="px-4">
          <ListaDestinos destinos={destinos} etiqueta={etiquetaNavegacion} />
        </div>
        <div className="mt-auto px-4 pb-4">{pie}</div>
      </aside>

      {rol === "persona" ? (
        <div className="sticky bottom-0 z-30 order-last h-[calc(var(--nav-inferior)+env(safe-area-inset-bottom))] border-t-2 border-border bg-card pb-[env(safe-area-inset-bottom)] lg:hidden">
          <nav aria-label={etiquetaNavegacion} className="px-2 py-1">
            <ul className="flex">
              {destinos.map((destino) => (
                <li key={destino.ruta} className="min-w-0 flex-1">
                  <NavLink to={destino.ruta} end={destino.exacta} className={ITEM_BARRA}>
                    <HugeiconsIcon icon={destino.icono} className={ICONO_ITEM_MENU} aria-hidden="true" />
                    <span className="sr-only">{destino.nombre}</span>
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      ) : textosCelularOperador ? (
        // A la derecha queda lugar para el botón de accesibilidad (flotante, lo monta el layout del panel).
        <header className="flex items-center border-b-2 border-border bg-secondary py-3 pr-[calc(var(--page-x)+calc(56rem/17))] pl-(--page-x) lg:hidden">
          <Sheet open={hojaAbierta} onOpenChange={setHojaAbierta}>
            <SheetTrigger render={<Button variant="outline" className="mb-0" />}>
              <HugeiconsIcon icon={ICONO_MENU} className="size-6" aria-hidden="true" />
              {textosCelularOperador.menu}
            </SheetTrigger>
            <SheetContent side="left" textoCerrar={textosCelularOperador.cerrarMenu} className="px-4 pb-6">
              <SheetHeader className="px-2">
                <SheetTitle>{textosCelularOperador.menu}</SheetTitle>
              </SheetHeader>
              <div className="flex flex-1 flex-col gap-6">
                <ListaDestinos
                  destinos={destinos}
                  etiqueta={etiquetaNavegacion}
                  alNavegar={() => setHojaAbierta(false)}
                />
                <div className="mt-auto">{pie}</div>
              </div>
            </SheetContent>
          </Sheet>
        </header>
      ) : null}
    </>
  )
}

/** Marca de arriba del menú de escritorio: avatar de 40 px, "Alma" y, si hay, el subtítulo. */
function Marca({ nombre, subtitulo, enlaceA }: MenuPrincipalProps["marca"]) {
  const contenido = (
    <>
      <AvatarAlma />
      <span className="flex min-w-0 flex-col">
        <span className="font-heading text-h3 font-medium text-heading">{nombre}</span>
        {subtitulo ? <span className="text-caption text-muted-foreground">{subtitulo}</span> : null}
      </span>
    </>
  )
  const clase = "flex items-center gap-3 px-4 py-6"
  return enlaceA ? (
    <Link to={enlaceA} className={clase}>
      {contenido}
    </Link>
  ) : (
    <div className={clase}>{contenido}</div>
  )
}

/** Lista vertical de destinos (barra lateral y hoja del operador). NavLink marca el activo con aria-current="page". */
function ListaDestinos({
  destinos,
  etiqueta,
  alNavegar,
}: {
  destinos: readonly DestinoMenu[]
  etiqueta: string
  alNavegar?: () => void
}) {
  return (
    <nav aria-label={etiqueta}>
      <ul className="flex flex-col gap-1">
        {destinos.map((destino) => (
          <li key={destino.ruta}>
            <NavLink to={destino.ruta} end={destino.exacta} onClick={alNavegar} className={ITEM_MENU}>
              <HugeiconsIcon icon={destino.icono} className={ICONO_ITEM_MENU} aria-hidden="true" />
              <span>{destino.nombre}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
