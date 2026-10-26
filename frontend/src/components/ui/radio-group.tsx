import { Radio as RadioPrimitive } from "@base-ui/react/radio"
import { RadioGroup as RadioGroupPrimitive } from "@base-ui/react/radio-group"
import { cn } from "cn"

// Grupo de radio: Base UI maneja el teclado (Tab entra al grupo, las flechas cambian la
// opción y Espacio elige). Cada opción se dibuja con su propio estilo (ver
// components/alma/opcion-accesibilidad.tsx); el foco es el anillo global de index.css.

function RadioGroup({ className, ...props }: RadioGroupPrimitive.Props) {
  return (
    <RadioGroupPrimitive
      data-slot="radio-group"
      className={cn("grid w-full gap-2", className)}
      {...props}
    />
  )
}

function RadioGroupItem({ className, ...props }: RadioPrimitive.Root.Props) {
  return (
    <RadioPrimitive.Root
      data-slot="radio-group-item"
      className={cn("cursor-pointer", className)}
      {...props}
    />
  )
}

export { RadioGroup, RadioGroupItem }
