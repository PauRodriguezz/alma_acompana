import type { IconSvgElement } from "@hugeicons/react"
import {
  BubbleChatIcon,
  Calendar03Icon,
  Home01Icon,
  Logout01Icon,
  Menu01Icon,
  Note01Icon,
  PuzzleIcon,
  User02Icon,
} from "@hugeicons/core-free-icons"

import { ICONO_HISTORIA_DE_VIDA } from "@/components/alma/iconos"
import {
  NAVEGACION_CHAT,
  NAVEGACION_ENTRETENIMIENTO,
  NAVEGACION_HISTORIA_DE_VIDA,
  NAVEGACION_PERFIL,
  NAVEGACION_RECORDATORIOS,
} from "@/lib/mensajes"

// Destinos del menú de cada rol y sus íconos: es el ÚNICO lugar donde se eligen. MenuPrincipal recibe
// el ícono desde acá y no importa ninguno por su cuenta. Los nombres son los de Hugeicons que usa el
// Figma (página Componentes, "Íconos Hugeicons (selección)"): bubble-chat, puzzle, book-open-02,
// calendar-03, user-02, home-01, note-01, logout-01 y menu-01.
//   - Ejercicios usa `puzzle` (decisión de Pau): el Figma le da `note-01`, igual que a Microcápsulas.
//     El Figma queda por actualizar en este punto.
//   - El Perfil del operador no está en el Figma: usa `user-02`, el mismo que el de la persona.

export type DestinoMenu = {
  ruta: string
  nombre: string
  icono: IconSvgElement
  /** Solo es el destino activo en esa ruta exacta (Chat, Panel). */
  exacta: boolean
}

export const RUTA_PERFIL = "/perfil"
export const RUTA_PERFIL_ADMIN = "/admin/perfil"

export const DESTINOS_PERSONA: readonly DestinoMenu[] = [
  { ruta: "/", nombre: NAVEGACION_CHAT, icono: BubbleChatIcon, exacta: true },
  { ruta: "/entretenimiento", nombre: NAVEGACION_ENTRETENIMIENTO, icono: PuzzleIcon, exacta: false },
  { ruta: "/historia-de-vida", nombre: NAVEGACION_HISTORIA_DE_VIDA, icono: ICONO_HISTORIA_DE_VIDA, exacta: false },
  { ruta: "/recordatorios", nombre: NAVEGACION_RECORDATORIOS, icono: Calendar03Icon, exacta: false },
  { ruta: RUTA_PERFIL, nombre: NAVEGACION_PERFIL, icono: User02Icon, exacta: false },
]

/** Los textos del operador salen de features/admin/mensajes.ts: el layout del panel los pasa armados. */
export function destinosOperador(textos: {
  panel: string
  ejercicios: string
  microcapsulas: string
  perfil: string
}): readonly DestinoMenu[] {
  return [
    { ruta: "/admin", nombre: textos.panel, icono: Home01Icon, exacta: true },
    { ruta: "/admin/ejercicios", nombre: textos.ejercicios, icono: PuzzleIcon, exacta: false },
    { ruta: "/admin/microcapsulas", nombre: textos.microcapsulas, icono: Note01Icon, exacta: false },
    { ruta: RUTA_PERFIL_ADMIN, nombre: textos.perfil, icono: User02Icon, exacta: false },
  ]
}

/** "Cerrar sesión" (ítem del pie del menú del operador) y el botón "Menú" del celular del operador. */
export const ICONO_CERRAR_SESION = Logout01Icon
export const ICONO_MENU = Menu01Icon
