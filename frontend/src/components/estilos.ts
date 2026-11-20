// Botón de ícono de las tablas del Figma (39:252): 48 px, fondo secondary, radio 12, ícono de
// 24 px (lápiz en heading, papelera en destructive). En alto contraste lleva borde.
// Siempre con nombre accesible (aria-label con la acción y el elemento).
export const estiloBotonIcono =
  "inline-flex size-(--altura-boton) shrink-0 cursor-pointer items-center justify-center rounded-md bg-secondary transition-colors hover:bg-secondary-hover contraste-alto:border-2 contraste-alto:border-foreground [&_svg]:size-6"

/** Mientras hay un pedido en curso el botón no hace nada: se ve igual que el botón de enviar deshabilitado. */
export const BOTON_EN_ESPERA =
  "aria-disabled:pointer-events-none aria-disabled:translate-y-0 aria-disabled:border-input aria-disabled:bg-disabled aria-disabled:text-disabled-foreground aria-disabled:shadow-none"

/**
 * Con letra "Muy grande" en 390 px, un texto largo se parte en varias líneas en vez de cortarse. El
 * botón nunca es más ancho que su contenedor (max-w-full): sin eso, a 360 px "Seguir escribiendo"
 * se salía de la tarjeta.
 */
export const BOTON_TEXTO_LARGO = "h-auto max-w-full whitespace-normal text-center"
