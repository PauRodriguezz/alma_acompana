import { Tabs as TabsPrimitive } from "@base-ui/react/tabs"
import { cn } from "@/lib/utils"

// Pestañas del Figma (Recordatorios: Lista y Calendario). Base UI da role="tablist", "tab" y
// "tabpanel", las flechas para moverse y el nombre accesible de cada panel. La pestaña activa
// se distingue por color, negrita y una línea gruesa debajo (nunca solo por el color); en
// alto contraste la línea es negra.

function Tabs({ className, ...props }: TabsPrimitive.Root.Props) {
  return <TabsPrimitive.Root data-slot="tabs" className={cn("flex flex-col gap-4", className)} {...props} />
}

function TabsList({ className, ...props }: TabsPrimitive.List.Props) {
  return (
    <TabsPrimitive.List
      data-slot="tabs-list"
      className={cn("flex border-b-2 border-border", className)}
      {...props}
    />
  )
}

function TabsTab({ className, ...props }: TabsPrimitive.Tab.Props) {
  return (
    <TabsPrimitive.Tab
      data-slot="tabs-tab"
      className={cn(
        "-mb-0.5 flex min-h-(--altura-boton) min-w-0 flex-1 cursor-pointer flex-wrap items-center justify-center gap-x-2 gap-y-1 border-b-4 px-2 text-center border-transparent py-2 font-heading text-button font-medium text-muted-foreground transition-colors hover:text-foreground data-active:border-accent data-active:font-semibold data-active:text-heading contraste-alto:data-active:border-foreground [&_svg]:shrink-0",
        className
      )}
      {...props}
    />
  )
}

function TabsPanel({ className, ...props }: TabsPrimitive.Panel.Props) {
  return <TabsPrimitive.Panel data-slot="tabs-panel" className={cn("outline-none", className)} {...props} />
}

export { Tabs, TabsList, TabsPanel, TabsTab }
