import { useCallback, useMemo, useState, type ReactNode, type RefObject } from "react"

import { AvisoFlotante } from "@/components/alma/aviso-flotante"

import { AvisoContext, type AvisoValor } from "./aviso-context"

type Aviso = { clave: number; texto: string }

let contador = 0

/**
 * Aviso flotante de éxito para cualquier módulo (`useAviso().avisar(texto)`). Se monta en el
 * layout de la persona mayor, que no se desmonta al cambiar de pantalla: el aviso sobrevive a
 * "guardar y volver a la lista". `contenidoRef` es el <main>: al cerrar con el botón, el foco
 * pasa ahí para no perderse.
 *
 * El contenedor es la región viva (role="status"), siempre montada, para que el lector de
 * pantalla anuncie el aviso cuando aparece.
 */
export function AvisoProvider({
  children,
  contenidoRef,
}: {
  children: ReactNode
  contenidoRef: RefObject<HTMLElement | null>
}) {
  const [aviso, setAviso] = useState<Aviso | null>(null)

  const avisar = useCallback((texto: string) => {
    contador += 1
    setAviso({ clave: contador, texto })
  }, [])
  const cerrar = useCallback(() => setAviso(null), [])
  const cerrarConBoton = useCallback(() => {
    setAviso(null)
    contenidoRef.current?.focus()
  }, [contenidoRef])

  const valor = useMemo<AvisoValor>(() => ({ avisar }), [avisar])

  return (
    <AvisoContext.Provider value={valor}>
      {children}
      <div
        role="status"
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-[calc(var(--nav-inferior)+env(safe-area-inset-bottom)+1rem)] z-40 flex justify-center pr-(--page-x) pl-[calc(var(--sidebar)+var(--page-x))]"
      >
        {aviso ? (
          <AvisoFlotante
            key={aviso.clave}
            texto={aviso.texto}
            onTiempo={cerrar}
            onCerrar={cerrarConBoton}
          />
        ) : null}
      </div>
    </AvisoContext.Provider>
  )
}
