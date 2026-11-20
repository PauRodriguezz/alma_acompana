// Estilo del ítem del menú lateral: es UNO solo para la persona y para el operador (los dos menús son
// idénticos en escritorio). Los valores son los del menú de la persona (Figma 40:273).
//
// - Ítem: 48 px de alto, ícono de 24 px (size-6), texto semibold, 12 px entre ícono y texto.
// - Activo: `aria-current="page"` (lo pone NavLink) con fondo accent-soft, barra de 4×24 px a la
//   izquierda y el ícono en accent. En alto contraste: borde de 2 px en foreground y fondo card. Todos
//   los ítems llevan borde transparente de 2 px solo en alto contraste, para que no cambien de tamaño.
export const ITEM_MENU =
  "group relative flex min-h-[calc(48rem/17)] w-full items-center gap-3 rounded-md px-4 py-2 text-left font-semibold text-foreground transition-colors hover:bg-secondary-hover " +
  "before:absolute before:top-1/2 before:left-0 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-accent before:opacity-0 " +
  "aria-[current=page]:bg-accent-soft aria-[current=page]:before:opacity-100 aria-[current=page]:hover:bg-accent-soft " +
  "contraste-alto:border-2 contraste-alto:border-transparent contraste-alto:aria-[current=page]:border-foreground contraste-alto:aria-[current=page]:bg-card"

export const ICONO_ITEM_MENU =
  "size-6 shrink-0 group-aria-[current=page]:text-accent contraste-alto:group-aria-[current=page]:text-foreground"

// Barra inferior del celular de la persona: SOLO ÍCONOS (el nombre queda para el lector de pantalla). Es una
// excepción documentada a "un ícono nunca va solo": los cinco nombres no entran en 1/5 del ancho de 360 px
// (menos con letra grande) y el Figma la dibuja así. A cambio: nombre para el lector de pantalla (sr-only),
// aria-current en el activo, objetivo táctil de 56 px de alto y 1/5 del ancho, y el activo distinguido por
// más que el color (fondo, barra arriba y, en alto contraste, borde de 2 px). El alto es fijo (--nav-inferior).
export const ITEM_BARRA =
  "group relative flex min-h-[calc(56rem/17)] flex-col items-center justify-center gap-1 rounded-md px-1 py-2 text-foreground transition-colors hover:bg-secondary " +
  "before:absolute before:top-0 before:left-1/2 before:h-1 before:w-6 before:-translate-x-1/2 before:rounded-full before:bg-accent before:opacity-0 " +
  "aria-[current=page]:bg-accent-soft aria-[current=page]:before:opacity-100 " +
  "contraste-alto:border-2 contraste-alto:border-transparent contraste-alto:aria-[current=page]:border-foreground contraste-alto:aria-[current=page]:bg-card"
