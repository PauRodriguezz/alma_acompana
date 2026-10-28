import { useState, type ReactElement } from "react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"

type Props = {
  /** El botón que abre el diálogo. */
  children: ReactElement
  titulo: string
  texto: string
  confirmar: string
  cancelar: string
  /** "destructive" para acciones que borran. */
  variante?: "primary" | "destructive"
  onConfirmar: () => void
}

/**
 * Confirmación antes de una acción que no se puede deshacer (eliminar un recordatorio, cerrar
 * sesión). Cancelar y Escape cierran sin hacer nada y el foco vuelve al botón que la abrió. Al
 * confirmar se cierra y llama a `onConfirmar`: quien la usa se ocupa de la acción.
 */
export function DialogoConfirmacion({
  children,
  titulo,
  texto,
  confirmar,
  cancelar,
  variante = "primary",
  onConfirmar,
}: Props) {
  const [abierto, setAbierto] = useState(false)

  return (
    <Dialog open={abierto} onOpenChange={setAbierto}>
      <DialogTrigger render={children} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{titulo}</DialogTitle>
        </DialogHeader>
        <DialogBody>
          <DialogDescription>{texto}</DialogDescription>
        </DialogBody>
        <DialogFooter className="border-t-0">
          <Button
            variant={variante}
            onClick={() => {
              setAbierto(false)
              onConfirmar()
            }}
          >
            {confirmar}
          </Button>
          <DialogClose render={<Button variant="outline" />}>{cancelar}</DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
