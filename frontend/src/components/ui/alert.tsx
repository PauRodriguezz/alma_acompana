import * as React from "react"
import { type VariantProps } from "class-variance-authority"
import { HugeiconsIcon } from "@hugeicons/react"
import {
  Alert02Icon,
  AlertCircleIcon,
  CheckmarkCircle02Icon,
  InformationCircleIcon,
} from "@hugeicons/core-free-icons"
import { cn } from "cn"

import { alertVariants } from "@/components/ui/alert-variants"

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
