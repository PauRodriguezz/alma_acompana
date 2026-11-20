// Clases de los botones del modo guion y de la tarjeta "Próxima parada" (contrato de historia de vida, 11.7).

/** Mientras hay un pedido en curso el botón no hace nada: se ve igual que el botón de enviar deshabilitado. */
export const BOTON_EN_ESPERA =
  "aria-disabled:pointer-events-none aria-disabled:translate-y-0 aria-disabled:border-input aria-disabled:bg-disabled aria-disabled:text-disabled-foreground aria-disabled:shadow-none"

/**
 * Con letra "Muy grande" en 390 px, un texto largo se parte en varias líneas en vez de cortarse. El
 * botón nunca es más ancho que su contenedor (max-w-full): sin eso, a 360 px "Seguir escribiendo"
 * se salía de la tarjeta.
 */
export const BOTON_TEXTO_LARGO = "h-auto max-w-full whitespace-normal text-center"
