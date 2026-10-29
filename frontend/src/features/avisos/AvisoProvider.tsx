import { useCallback, useMemo, useState, type ReactNode, type RefObject } from "react"

import { AvisoFlotante } from "@/components/alma/aviso-flotante"

import { AvisoContext, type AvisoValor } from "./aviso-context"

type Aviso = { clave: number; texto: string }

let contador = 0

// Siempre centrado en el contenido (a la derecha del menú lateral, que mide --sidebar). Cambia
// solo la altura sobre el borde de abajo:
// - persona: arriba de la barra inferior del celular (--nav-inferior).
// - panel: el celular no tiene barra inferior; en escritorio el botón de accesibilidad
//   (44 px, a 24 px del borde, ver BotonAccesibilidad) queda abajo a la derecha y el aviso va
//   por encima de él.
const POSICION = {
  persona: "bottom-[calc(var(--nav-inferior)+env(safe-area-inset-bottom)+1rem)]",
  panel:
    "bottom-[calc(env(safe-area-inset-bottom)+1rem)] lg:bottom-[calc(1.5rem+calc(44rem/17)+1rem)]",
} as const

/**
 * Aviso flotante de éxito para cualquier módulo (`useAviso().avisar(texto)`). Se monta en el
 * layout de la persona mayor y en el del panel de operador, que no se desmontan al cambiar de
 * pantalla: el aviso sobrevive a "guardar y volver a la lista". `contenidoRef` es el <main>: al
 * cerrar con el botón, el foco pasa ahí para no perderse.
 *
 * El contenedor es la región viva (role="status"), siempre montada, para que el lector de
 * pantalla anuncie el aviso cuando aparece.
 */
export function AvisoProvider({
  children,
  contenidoRef,
  posicion = "persona",
}: {
  children: ReactNode
  contenidoRef: RefObject<HTMLElement | null>
  posicion?: keyof typeof POSICION
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
        className={`pointer-events-none fixed inset-x-0 ${POSICION[posicion]} z-40 flex justify-center pr-(--page-x) pl-[calc(var(--sidebar)+var(--page-x))]`}
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
