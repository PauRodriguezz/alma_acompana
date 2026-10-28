import { mergeProps } from "@base-ui/react/merge-props"
import { useRender } from "@base-ui/react/use-render"
import { cn } from "cn"

// Chip del Figma (21:107): "Etiqueta de tipo o categoría. Siempre lleva texto; ícono cuando
// el tipo importa. En alto contraste: fondo card y borde de 2 px."
// Píldora con texto label semibold en foreground, 12 px a los costados y 4 px arriba y abajo.
// El fondo lo pone quien la usa (por ejemplo bg-info-soft): ver features/admin/etiquetas.ts.
// El borde transparente de 2 px hace que en alto contraste no cambie de tamaño.
// El color de texto se hereda (foreground): cn descarta text-label si en la misma llamada
// hay un color de texto, así que no se pone uno acá ni se pasa por className.
function Badge({ className, render, ...props }: useRender.ComponentProps<"span">) {
  return useRender({
    defaultTagName: "span",
    props: mergeProps<"span">(
      {
        className: cn(
          "inline-flex w-fit shrink-0 items-center gap-2 rounded-full border-2 border-transparent bg-secondary px-3 py-0.5 text-label font-semibold whitespace-nowrap contraste-alto:border-foreground contraste-alto:bg-card [&>svg]:pointer-events-none [&>svg]:size-4 [&>svg]:shrink-0",
          className
        ),
      },
      props
    ),
    render,
    state: {
      slot: "badge",
    },
  })
}

export { Badge }
