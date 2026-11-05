type Props = {
  texto: string
}

/**
 * Mensaje escrito por la persona (Figma: "Burbuja de chat"): burbuja alineada a la derecha, como la de
 * `BurbujaAnimo` pero con el texto. Con alto contraste lleva borde. El texto se muestra siempre como
 * texto plano (React lo escapa): nunca como HTML.
 */
export function MensajePersona({ texto }: Props) {
  return (
    <div className="flex justify-end">
      <p className="max-w-[85%] rounded-lg bg-accent-soft px-4 py-3 break-words whitespace-pre-line text-foreground contraste-alto:border-2 contraste-alto:border-foreground">
        {texto}
      </p>
    </div>
  )
}
