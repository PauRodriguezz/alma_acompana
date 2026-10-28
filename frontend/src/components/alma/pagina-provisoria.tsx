const TEXTO_PROVISORIO = "Esta pantalla todavía se está armando."

/** Pantalla provisoria para probar la navegación; cada módulo la reemplaza por la suya. */
export function PaginaProvisoria({ titulo }: { titulo: string }) {
  return (
    <div className="flex flex-col items-start gap-2">
      <h1>{titulo}</h1>
      <p className="text-muted-foreground">{TEXTO_PROVISORIO}</p>
    </div>
  )
}
