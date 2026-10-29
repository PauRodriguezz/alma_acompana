import { NavLink } from "react-router-dom"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  BookOpen01Icon,
  Home01Icon,
  Logout03Icon,
  PuzzleIcon,
  UserIcon,
} from "@hugeicons/core-free-icons"
import { cn } from "cn"

import { DialogoCerrarSesion } from "@/features/auth/DialogoCerrarSesion"

// Ítem del menú lateral del Figma (40:321): 56 px de alto, ícono de 28 px, label semibold.
// Activo: fondo accent-soft y una barra accent de 5 px a la izquierda (no solo color).
const estiloItemMenu =
  "relative flex min-h-[calc(56rem/17)] w-full items-center gap-4 rounded-md px-4 py-2 text-left text-label font-semibold transition-colors hover:bg-secondary-hover"

const estiloItemActivo =
  "bg-accent-soft hover:bg-accent-soft before:absolute before:top-2 before:bottom-2 before:left-0 before:w-[calc(5rem/17)] before:rounded-r-[calc(3rem/17)] before:bg-accent"

// Íconos: el Figma usa el mismo para Ejercicios y Microcápsulas; acá cada sección tiene el
// suyo para distinguirlas mejor. Perfil no está en el Figma (contrato, sección 1).
const ITEMS = [
  { a: "/admin", texto: "Panel", icono: Home01Icon, exacto: true },
  { a: "/admin/ejercicios", texto: "Ejercicios", icono: PuzzleIcon, exacto: false },
  { a: "/admin/microcapsulas", texto: "Microcápsulas", icono: BookOpen01Icon, exacto: false },
  { a: "/admin/perfil", texto: "Perfil", icono: UserIcon, exacto: false },
]

/**
 * Opciones del menú del panel y "Cerrar sesión" abajo. Es el mismo contenido en el menú
 * lateral de escritorio y en el panel de celular. NavLink marca la sección activa con
 * aria-current="page".
 */
export function MenuAdmin({ alNavegar }: { alNavegar?: () => void }) {
  return (
    <div className="flex flex-1 flex-col gap-6">
      <nav aria-label="Menú del panel">
        <ul className="flex flex-col gap-2">
          {ITEMS.map((item) => (
            <li key={item.a}>
              <NavLink
                to={item.a}
                end={item.exacto}
                onClick={alNavegar}
                className={({ isActive }) => cn(estiloItemMenu, isActive && estiloItemActivo)}
              >
                <HugeiconsIcon icon={item.icono} className="size-7 shrink-0" aria-hidden="true" />
                <span>{item.texto}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="mt-auto">
        <DialogoCerrarSesion
          disparador={
            <button type="button" className={cn(estiloItemMenu, "cursor-pointer")}>
              <HugeiconsIcon icon={Logout03Icon} className="size-7 shrink-0" aria-hidden="true" />
              <span>Cerrar sesión</span>
            </button>
          }
        />
      </div>
    </div>
  )
}
