import { cva } from "class-variance-authority"

// Botón del Figma (página Componentes): 48 px de alto mínimo y borde inferior de 4 px
// (--edge) que se "hunde" al apretar. El foco es el anillo global de index.css, con
// separación (el borde de 3 px sin separación del Figma no se ve contra primary).
export const buttonVariants = cva(
  "inline-flex min-h-(--altura-boton) shrink-0 items-center justify-center gap-2 rounded-md border-2 px-3.5 py-2.5 mb-(--borde-boton) font-heading text-button font-medium whitespace-nowrap select-none transition-[background-color,box-shadow,translate] duration-100 shadow-[0_var(--borde-boton)_0_0_var(--edge)] active:translate-y-(--borde-boton) active:shadow-none disabled:pointer-events-none disabled:translate-y-0 disabled:border-input disabled:bg-disabled disabled:text-disabled-foreground disabled:shadow-none [&_svg]:pointer-events-none [&_svg]:shrink-0",
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
    },
    defaultVariants: {
      variant: "primary",
    },
  }
)
