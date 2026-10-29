import {
  BookOpen01Icon,
  Calendar03Icon,
  Message01Icon,
  PuzzleIcon,
  UserIcon,
} from "@hugeicons/core-free-icons"

import {
  NAVEGACION_CHAT,
  NAVEGACION_ENTRETENIMIENTO,
  NAVEGACION_HISTORIA_DE_VIDA,
  NAVEGACION_PERFIL,
  NAVEGACION_RECORDATORIOS,
} from "@/lib/mensajes"

export const RUTA_PERFIL = "/perfil"

/** Las cinco entradas de la navegación principal de la persona mayor, en este orden. */
export const ENTRADAS_NAVEGACION = [
  // El Chat es "/": solo cuenta como activo en esa ruta exacta.
  { ruta: "/", nombre: NAVEGACION_CHAT, icono: Message01Icon, exacta: true },
  { ruta: "/entretenimiento", nombre: NAVEGACION_ENTRETENIMIENTO, icono: PuzzleIcon, exacta: false },
  { ruta: "/historia-de-vida", nombre: NAVEGACION_HISTORIA_DE_VIDA, icono: BookOpen01Icon, exacta: false },
  { ruta: "/recordatorios", nombre: NAVEGACION_RECORDATORIOS, icono: Calendar03Icon, exacta: false },
  { ruta: RUTA_PERFIL, nombre: NAVEGACION_PERFIL, icono: UserIcon, exacta: false },
] as const
