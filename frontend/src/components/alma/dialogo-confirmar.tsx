import { useState } from "react"
import type * as React from "react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"

type DialogoConfirmarProps = {
  titulo: string
  descripcion?: string
  textoConfirmar: string
  /** destructive para las bajas (Figma 52:2333); primary para el resto. */
  variante?: "primary" | "destructive"
  alConfirmar: () => void
  /** true mientras se procesa: el botón de confirmar queda deshabilitado. */
  procesando?: boolean
  /** Botón que abre el diálogo (va como render del DialogTrigger). */
  disparador?: React.ReactElement
  /** Uso controlado (sin disparador). */
  open?: boolean
  onOpenChange?: (abierto: boolean) => void
}

/**
 * Diálogo de confirmación del Figma (Ejercicios · confirmar eliminar, 52:2333): título,
 * texto y los botones en fila a la derecha: Cancelar y la acción. En celular los botones
 * van uno debajo del otro, a lo ancho. Lo accesible (foco atrapado, Escape, el foco
 * vuelve al botón que lo abrió) lo pone Base UI (ver components/ui/dialog.tsx).
 * Al confirmar, el diálogo se cierra y después se llama a alConfirmar: el resultado (aviso
 * de éxito o de error) lo muestra la pantalla.
 */
export function DialogoConfirmar({
  titulo,
  descripcion,
  textoConfirmar,
  variante = "primary",
  alConfirmar,
  procesando = false,
  disparador,
  open,
  onOpenChange,
}: DialogoConfirmarProps) {
  const [abiertoInterno, setAbiertoInterno] = useState(false)
  const abierto = open ?? abiertoInterno

  function cambiarAbierto(valor: boolean) {
    setAbiertoInterno(valor)
    onOpenChange?.(valor)
  }

  function confirmar() {
    cambiarAbierto(false)
    alConfirmar()
  }

  return (
    <Dialog open={abierto} onOpenChange={cambiarAbierto}>
      {disparador ? <DialogTrigger render={disparador} /> : null}
      <DialogContent>
        <DialogHeader className="gap-3">
          {/* DialogTitle es un <h2>: el tamaño h2 sale de index.css. Solo se ajusta en escritorio. */}
          <DialogTitle className="lg:text-h2-desktop">{titulo}</DialogTitle>
          {descripcion ? (
            <DialogDescription className="text-foreground">{descripcion}</DialogDescription>
          ) : null}
        </DialogHeader>
        <DialogFooter className="border-t-0 pt-2 sm:flex-row sm:flex-wrap sm:justify-end">
          <DialogClose render={<Button variant="outline" />}>
            Cancelar
          </DialogClose>
          <Button
            variant={variante}
            onClick={confirmar}
            disabled={procesando}
          >
            {textoConfirmar}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
