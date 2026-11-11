import { useEffect, useRef, type ReactNode } from "react"

/** El h1 de la pantalla, que recibe el foco al aparecer (contrato de microcápsulas, 10.4). */
export function TituloConFoco({ children }: { children: ReactNode }) {
  const ref = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    ref.current?.focus({ preventScroll: true })
  }, [])
  return (
    <h1 ref={ref} tabIndex={-1} className="wrap-break-word outline-none lg:text-h1-desktop">
      {children}
    </h1>
  )
}
