import { useEffect, useRef } from "react"
import { Link } from "react-router-dom"

import almaSaludo from "@/assets/alma_acompana_saludo.webp"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { RUTA_ENTRETENIMIENTO } from "@/features/entretenimiento/rutas"

import { BOTON_SALIR, BOTON_SEGUIR_JUGANDO, FINAL_TEXTO, jugasteHoy } from "../mensajes"

// Confeti del Figma (30:521): puntos y tiras con los colores de estado, quieto (sin animación) y
// solo decorativo. Las clases van escritas enteras para que Tailwind las encuentre.
const CONFETI: { top: string; left: string; forma: "punto" | "tira"; color: string; giro: number }[] = [
  { top: "4%", left: "14%", forma: "punto", color: "bg-accent", giro: 0 },
  { top: "3%", left: "52%", forma: "tira", color: "bg-warning", giro: -30 },
  { top: "5%", left: "70%", forma: "tira", color: "bg-primary", giro: 55 },
  { top: "7%", left: "88%", forma: "tira", color: "bg-success", giro: -35 },
  { top: "8%", left: "5%", forma: "punto", color: "bg-primary", giro: 0 },
  { top: "7%", left: "29%", forma: "punto", color: "bg-success", giro: 0 },
  { top: "12%", left: "40%", forma: "punto", color: "bg-primary", giro: 0 },
  { top: "12%", left: "61%", forma: "tira", color: "bg-info", giro: 20 },
  { top: "16%", left: "18%", forma: "tira", color: "bg-accent", giro: 10 },
  { top: "16%", left: "77%", forma: "punto", color: "bg-info", giro: 0 },
  { top: "18%", left: "93%", forma: "punto", color: "bg-accent", giro: 0 },
  { top: "19%", left: "4%", forma: "tira", color: "bg-info", giro: 70 },
  { top: "25%", left: "90%", forma: "tira", color: "bg-heading", giro: 40 },
  { top: "28%", left: "10%", forma: "tira", color: "bg-warning", giro: 15 },
  { top: "35%", left: "82%", forma: "tira", color: "bg-warning", giro: 90 },
  { top: "37%", left: "22%", forma: "tira", color: "bg-heading", giro: 50 },
  { top: "39%", left: "9%", forma: "tira", color: "bg-heading", giro: -50 },
  { top: "40%", left: "86%", forma: "tira", color: "bg-success", giro: 10 },
]

/**
 * Final de la ronda (contrato, 10.2; Figma 30:521): Alma festejando (la imagen de saludo),
 * "Jugaste N juegos hoy", "Seguir jugando" (pide el siguiente) y "Salir" (vuelve a Entretenimiento).
 * Nunca se muestra puntaje. En escritorio, que no tiene diseño, va dentro de una tarjeta.
 */
export function FinalRonda({
  jugadosHoy,
  onSeguirJugando,
}: {
  jugadosHoy: number
  onSeguirJugando: () => void
}) {
  const tituloRef = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    window.scrollTo({ top: 0 })
    tituloRef.current?.focus({ preventScroll: true })
  }, [])

  return (
    <div className="relative flex flex-col items-center gap-4 overflow-hidden pt-[calc(96rem/17)] text-center lg:max-w-(--narrow-max) lg:rounded-xl lg:bg-card lg:p-6 lg:pt-[calc(112rem/17)] lg:shadow-sm contraste-alto:lg:border-2 contraste-alto:lg:border-foreground">
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 contraste-alto:hidden">
        {CONFETI.map((pieza, i) => (
          <span
            key={i}
            className={`absolute ${pieza.color} ${
              pieza.forma === "punto"
                ? "size-[calc(10rem/17)] rounded-full"
                : "h-[calc(14rem/17)] w-[calc(7rem/17)] rounded-[calc(2rem/17)]"
            }`}
            style={{ top: pieza.top, left: pieza.left, rotate: `${pieza.giro}deg` }}
          />
        ))}
      </div>

      <img
        src={almaSaludo}
        alt=""
        className="relative h-[calc(150rem/17)] w-auto object-contain lg:h-[calc(176rem/17)]"
      />
      <h1 ref={tituloRef} tabIndex={-1} className="relative outline-none lg:text-h1-desktop">
        {jugasteHoy(jugadosHoy)}
      </h1>
      <p className="relative text-muted-foreground">{FINAL_TEXTO}</p>

      <div className="relative mt-4 grid w-full gap-3 lg:grid-cols-2">
        <Button className="w-full" onClick={onSeguirJugando}>
          {BOTON_SEGUIR_JUGANDO}
        </Button>
        <Link
          to={RUTA_ENTRETENIMIENTO}
          className={buttonVariants({ variant: "outline", className: "w-full" })}
        >
          {BOTON_SALIR}
        </Link>
      </div>
    </div>
  )
}
