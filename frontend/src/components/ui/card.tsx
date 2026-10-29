import * as React from "react"
import { cn } from "@/lib/utils"

// Tarjeta del Figma (tablas y formularios del panel de operador): fondo card, radio 24,
// sombra shadow-sm y 24 px de relleno. En alto contraste no hay sombras: lleva un borde
// de 2 px en foreground, como el diálogo.

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card"
      className={cn(
        "flex flex-col gap-4 rounded-xl bg-card p-6 text-body shadow-sm contraste-alto:border-2 contraste-alto:border-foreground",
        className
      )}
      {...props}
    />
  )
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div data-slot="card-header" className={cn("flex flex-col gap-1", className)} {...props} />
  )
}

// Es un div como en shadcn: quien la usa elige el nivel de título con un h2/h3 adentro.
// El tamaño va como text-(length:…): con text-h2-desktop, cn lo descartaría por text-heading.
function CardTitle({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-title"
      className={cn(
        "font-heading text-(length:--text-h2-desktop) leading-(--text-h2-desktop--line-height) font-medium text-heading",
        className
      )}
      {...props}
    />
  )
}

function CardDescription({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-description"
      className={cn("text-muted-foreground", className)}
      {...props}
    />
  )
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return <div data-slot="card-content" className={cn(className)} {...props} />
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="card-footer"
      className={cn("flex flex-wrap items-center gap-3", className)}
      {...props}
    />
  )
}

export { Card, CardHeader, CardFooter, CardTitle, CardDescription, CardContent }
