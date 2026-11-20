import { Button as ButtonPrimitive } from "@base-ui/react/button"
import { type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

import { buttonVariants } from "./button-variants"

function Button({
  className,
  variant = "primary",
  tamanio,
  enEspera,
  ...props
}: ButtonPrimitive.Props & VariantProps<typeof buttonVariants>) {
  return (
    <ButtonPrimitive
      data-slot="button"
      className={cn(buttonVariants({ variant, tamanio, enEspera }), className)}
      {...props}
    />
  )
}

export { Button }
