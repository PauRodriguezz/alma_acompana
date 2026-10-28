import type { ReactNode } from "react"
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "cn"

// Etiqueta corta del Figma ("Medicación", "Pendiente", "Vista"...): siempre lleva texto (y a
// veces un ícono), así que el estado nunca depende solo del color. En alto contraste, borde negro.
const chipVariants = cva(
  "inline-flex items-center gap-1 rounded-full px-3 py-1 text-caption font-semibold text-foreground contraste-alto:border-2 contraste-alto:border-foreground contraste-alto:bg-card",
  {
    variants: {
      tono: {
        info: "bg-info-soft",
        neutro: "bg-secondary",
        acento: "bg-accent-soft",
      },
    },
    defaultVariants: { tono: "neutro" },
  }
)

export function Chip({
  tono,
  icono,
  children,
  className,
}: VariantProps<typeof chipVariants> & {
  icono?: IconSvgElement
  children: ReactNode
  className?: string
}) {
  return (
    <span className={cn(chipVariants({ tono }), className)}>
      {icono ? <HugeiconsIcon icon={icono} className="size-4 shrink-0" aria-hidden="true" /> : null}
      {children}
    </span>
  )
}
