import { cva } from "class-variance-authority"

// Aviso del Figma: color de fondo + ícono + texto, nunca solo color. En alto contraste
// el fondo es blanco y lleva un borde de 2 px del color del aviso. El de error siempre
// lleva borde destructive.
export const alertVariants = cva(
  "flex w-full items-start gap-3 rounded-md border-2 border-transparent p-4 text-body text-foreground contraste-alto:bg-card",
  {
    variants: {
      variant: {
        exito: "bg-success-soft contraste-alto:border-success [&>svg]:text-success",
        atencion:
          "bg-warning-soft contraste-alto:border-warning [&>svg]:text-warning",
        info: "bg-info-soft contraste-alto:border-info [&>svg]:text-heading",
        error: "border-destructive bg-card [&>svg]:text-destructive",
      },
    },
    defaultVariants: {
      variant: "info",
    },
  }
)
