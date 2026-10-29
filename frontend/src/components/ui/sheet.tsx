import * as React from "react"
import { Dialog as SheetPrimitive } from "@base-ui/react/dialog"
import { HugeiconsIcon } from "@hugeicons/react"
import { Cancel01Icon } from "@hugeicons/core-free-icons"
import { cn } from "@/lib/utils"

import { Button } from "@/components/ui/button"

// Panel lateral (menú del panel de operador en celular). PROPUESTO: el Figma del operador
// es solo de escritorio. Es un diálogo de Base UI, así que hereda lo accesible del Dialog:
// foco atrapado, Escape cierra y el foco vuelve al botón que lo abrió. Fondo y velo como
// el diálogo (dialog.tsx); en alto contraste, borde de 2 px en foreground.

function Sheet({ ...props }: SheetPrimitive.Root.Props) {
  return <SheetPrimitive.Root data-slot="sheet" {...props} />
}

function SheetTrigger({ ...props }: SheetPrimitive.Trigger.Props) {
  return <SheetPrimitive.Trigger data-slot="sheet-trigger" {...props} />
}

function SheetClose({ ...props }: SheetPrimitive.Close.Props) {
  return <SheetPrimitive.Close data-slot="sheet-close" {...props} />
}

function SheetContent({
  className,
  children,
  side = "left",
  textoCerrar = "Cerrar",
  ...props
}: SheetPrimitive.Popup.Props & {
  side?: "left" | "right"
  /** Nombre accesible del botón de cerrar (la X). */
  textoCerrar?: string
}) {
  return (
    <SheetPrimitive.Portal data-slot="sheet-portal">
      <SheetPrimitive.Backdrop
        data-slot="sheet-overlay"
        className="fixed inset-0 z-50 bg-foreground/60 transition-opacity duration-150 data-ending-style:opacity-0 data-starting-style:opacity-0"
      />
      <SheetPrimitive.Popup
        data-slot="sheet-content"
        data-side={side}
        className={cn(
          "fixed inset-y-0 z-50 flex h-full w-[min(85%,calc(360rem/17))] flex-col overflow-y-auto bg-popover text-body outline-none [box-shadow:var(--sombra-tarjeta)] transition duration-200 ease-in-out contraste-alto:border-foreground data-ending-style:opacity-0 data-starting-style:opacity-0 data-[side=left]:left-0 data-[side=left]:contraste-alto:border-r-2 data-[side=left]:data-ending-style:-translate-x-10 data-[side=left]:data-starting-style:-translate-x-10 data-[side=right]:right-0 data-[side=right]:contraste-alto:border-l-2 data-[side=right]:data-ending-style:translate-x-10 data-[side=right]:data-starting-style:translate-x-10",
          className
        )}
        {...props}
      >
        {children}
        <SheetPrimitive.Close
          data-slot="sheet-close"
          aria-label={textoCerrar}
          render={
            <Button
              variant="secondary"
              className="absolute top-4 right-4 size-(--altura-boton) p-0"
            />
          }
        >
          <HugeiconsIcon icon={Cancel01Icon} className="size-6" aria-hidden="true" />
        </SheetPrimitive.Close>
      </SheetPrimitive.Popup>
    </SheetPrimitive.Portal>
  )
}

function SheetHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-header"
      className={cn("flex flex-col gap-1 p-6 pr-20", className)}
      {...props}
    />
  )
}

function SheetFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="sheet-footer"
      className={cn("mt-auto flex flex-col gap-3 p-6", className)}
      {...props}
    />
  )
}

function SheetTitle({ className, ...props }: SheetPrimitive.Title.Props) {
  return (
    <SheetPrimitive.Title
      data-slot="sheet-title"
      // Es un <h2>: el tamaño h2 sale de index.css (con text-h2 acá, cn lo descartaría por text-heading)
      className={cn("font-heading font-medium text-heading", className)}
      {...props}
    />
  )
}

function SheetDescription({ className, ...props }: SheetPrimitive.Description.Props) {
  return (
    <SheetPrimitive.Description
      data-slot="sheet-description"
      className={cn("text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Sheet,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
}
