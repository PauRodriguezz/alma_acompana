import { cva } from "class-variance-authority"

// Botón del Figma (página Componentes): 48 px de alto mínimo y borde inferior de 4 px
// (--edge) que se "hunde" al apretar. El foco es el anillo global de index.css, con
// separación (el borde de 3 px sin separación del Figma no se ve contra primary).
export const buttonVariants = cva(
  "inline-flex min-h-(--altura-boton) shrink-0 items-center justify-center gap-2 rounded-md border-2 px-3.5 py-2.5 mb-(--borde-boton) font-heading text-button font-medium select-none transition-[background-color,box-shadow,translate] duration-100 shadow-[0_var(--borde-boton)_0_0_var(--edge)] active:translate-y-(--borde-boton) active:shadow-none disabled:pointer-events-none disabled:translate-y-0 disabled:border-input disabled:bg-disabled disabled:text-disabled-foreground disabled:shadow-none [&_svg]:pointer-events-none [&_svg]:shrink-0",
  {
    variants: {
      variant: {
        primary:
          "[--edge:var(--primary-pressed)] border-transparent bg-primary text-primary-foreground hover:bg-primary-hover active:bg-primary-pressed",
        secondary:
          "[--edge:var(--secondary-edge)] border-transparent bg-secondary text-secondary-foreground hover:bg-secondary-hover",
        outline:
          "[--edge:var(--input)] border-input bg-card text-heading hover:bg-outline-hover",
        destructive:
          "[--edge:var(--destructive-edge)] border-transparent bg-destructive text-destructive-foreground hover:bg-destructive-hover",
      },
      // Con letra "Muy grande" en 390 px, un texto largo se parte en varias líneas en vez de cortarse. El botón
      // nunca es más ancho que su contenedor (max-w-full): sin eso, a 360 px "Seguir escribiendo" se salía de la tarjeta.
      tamanio: {
        normal: "whitespace-nowrap",
        textoLargo: "h-auto max-w-full whitespace-normal text-center",
      },
      // Mientras hay un pedido en curso el botón no hace nada (usa aria-disabled, no disabled, para no perder el
      // foco): se ve igual que el botón de enviar deshabilitado.
      enEspera: {
        true: "aria-disabled:pointer-events-none aria-disabled:translate-y-0 aria-disabled:border-input aria-disabled:bg-disabled aria-disabled:text-disabled-foreground aria-disabled:shadow-none",
        false: "",
      },
    },
    defaultVariants: {
      variant: "primary",
      tamanio: "normal",
      enEspera: false,
    },
  }
)

// Botón de ícono de las tablas del Figma (39:252): 48 px, fondo secondary, radio 12, ícono de 24 px (lápiz en
// heading, papelera en destructive). En alto contraste lleva borde. Siempre con nombre accesible (aria-label con
// la acción y el elemento).
export const botonIconoVariants = cva(
  "inline-flex size-(--altura-boton) shrink-0 cursor-pointer items-center justify-center rounded-md bg-secondary transition-colors hover:bg-secondary-hover contraste-alto:border-2 contraste-alto:border-foreground [&_svg]:size-6",
  {
    variants: {
      tono: {
        editar: "text-heading",
        eliminar: "text-destructive",
      },
    },
    defaultVariants: {
      tono: "editar",
    },
  }
)
