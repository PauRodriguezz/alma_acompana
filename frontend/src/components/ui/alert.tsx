import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  Alert02Icon,
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  InformationCircleIcon,
} from "@hugeicons/core-free-icons"
import { cn } from "cn"

// Aviso del Figma: color de fondo + ícono + texto, nunca solo color. En alto contraste
// el fondo es blanco y lleva un borde de 2 px del color del aviso. El de error siempre
// lleva borde destructive.
const alertVariants = cva(
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

type Variante = NonNullable<VariantProps<typeof alertVariants>["variant"]>

const iconos = {
  exito: CheckmarkCircle02Icon,
  atencion: Alert02Icon,
  info: InformationCircleIcon,
  error: AlertCircleIcon,
} satisfies Record<Variante, unknown>

function Alert({
  className,
  variant = "info",
  children,
  ...props
}: React.ComponentProps<"div"> & VariantProps<typeof alertVariants>) {
  const tipo: Variante = variant ?? "info"
  return (
    <div
      data-slot="alert"
      role={tipo === "error" ? "alert" : "status"}
      className={cn(alertVariants({ variant: tipo }), className)}
      {...props}
    >
      <HugeiconsIcon
        icon={iconos[tipo]}
        className="size-6 shrink-0"
        aria-hidden="true"
      />
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}

export { Alert }
