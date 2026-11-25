import { useEffect, useId, useRef, useState } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { Cancel01Icon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

import { elegirAviso } from "../avisoDeHoy"
import {
  AVISO_PARA_MAS_TARDE,
  CERRAR_AVISO,
  MOSTRAR_DE_NUEVO,
  MOSTRAR_DE_NUEVO_AVISO,
  PARA_HOY,
  verLos,
} from "../mensajes"
import type { EstadoNotificacion, ItemAgenda } from "../tipos"

import { TarjetaAviso } from "./Tarjetas"

// La misma caja para el aviso y para la card de "más tarde"
const CLASES_TARJETA =
  "flex rounded-lg bg-card p-4 [box-shadow:var(--sombra-sm)] contraste-alto:border-2 contraste-alto:border-foreground"

type Props = {
  /** La agenda de hoy completa (useAgendaDeHoy): la tarjeta elige sola qué avisar. */
  agendaDeHoy: ItemAgenda[]
  /** "Ahora" en hora de Argentina (useAhora), para "Pasó hace 2 h". */
  ahora: Date
  /** Id del aviso que se está guardando, para deshabilitar su botón. */
  ocupadoId?: number
  onCambiarEstado: (item: ItemAgenda, estado: EstadoNotificacion) => void
}

/**
 * Tarjeta "Para hoy" (CU-09, regla 5.7): avisa de la primera notificación pendiente del día. Si
 * hay varias en la misma franja horaria, las muestra juntas como un único aviso, con la misma
 * tarjeta de la Lista. Medicación: "Ya la tomé". Cita: la tarjeta entera lleva a su detalle (donde
 * se marca como vista).
 *
 * La X la oculta "por ahora": no cambia ningún estado ni agenda nada, y el aviso queda oculto solo
 * en memoria (no se guarda en el navegador). En su lugar queda una card chica, que no desaparece
 * sola, con "Listo, lo dejo para más tarde." y "Mostrar de nuevo". El aviso vuelve con ese botón,
 * al recargar la página, al volver a ingresar a la app (también si se sale de esta pantalla y se
 * vuelve) o cuando otro aviso pasa a ser el primero (entonces la card se reemplaza).
 * Foco: al cerrar pasa a "Mostrar de nuevo" (el botón X desaparece) y al mostrar de nuevo, al
 * título; solo cuando el cambio lo provoca la persona.
 *
 * Es reutilizable: el módulo de Chat la monta en su saludo con los datos de useAgendaDeHoy.
 */
export function TarjetaParaHoy({ agendaDeHoy, ahora, ocupadoId, onCambiarEstado }: Props) {
  const aviso = elegirAviso(agendaDeHoy)
  const [claveOculta, setClaveOculta] = useState<string | null>(null)
  const idMensaje = useId()
  const tituloRef = useRef<HTMLHeadingElement>(null)
  const mostrarRef = useRef<HTMLButtonElement>(null)
  const pedirFoco = useRef(false)

  const oculta = aviso !== null && aviso.clave === claveOculta

  useEffect(() => {
    if (!pedirFoco.current) return
    pedirFoco.current = false
    if (oculta) mostrarRef.current?.focus()
    else tituloRef.current?.focus()
  }, [oculta])

  if (!aviso) return null

  if (oculta) {
    return (
      <div role="status" className={`${CLASES_TARJETA} flex-row flex-wrap items-center justify-between gap-x-4 gap-y-1`}>
        <p id={idMensaje}>{AVISO_PARA_MAS_TARDE}</p>
        <button
          ref={mostrarRef}
          type="button"
          aria-label={MOSTRAR_DE_NUEVO_AVISO}
          aria-describedby={idMensaje}
          onClick={() => {
            pedirFoco.current = true
            setClaveOculta(null)
          }}
          className="inline-flex min-h-12 items-center font-semibold text-heading underline underline-offset-4"
        >
          {MOSTRAR_DE_NUEVO}
        </button>
      </div>
    )
  }

  const hayMas = agendaDeHoy.length > aviso.items.length

  return (
    <section
      aria-labelledby="para-hoy-titulo"
      className={`${CLASES_TARJETA} flex-col gap-3`}
    >
      <div className="flex items-center justify-between gap-3">
        <h2 id="para-hoy-titulo" ref={tituloRef} tabIndex={-1} className="text-h3 outline-none">
          {PARA_HOY}
        </h2>
        <button
          type="button"
          aria-label={CERRAR_AVISO}
          onClick={() => {
            pedirFoco.current = true
            setClaveOculta(aviso.clave)
          }}
          className="-my-2 -mr-2 inline-flex size-12 shrink-0 items-center justify-center rounded-md text-foreground hover:bg-secondary"
        >
          <HugeiconsIcon icon={Cancel01Icon} className="size-6" aria-hidden="true" />
        </button>
      </div>

      <p className="font-heading text-h1 font-medium text-heading">{aviso.hora}</p>

      <ul className="flex flex-col gap-3">
        {aviso.items.map((item) => (
          <li key={item.notificacionId}>
            <TarjetaAviso
              item={item}
              ahora={ahora}
              mostrarHora={false}
              ocupado={ocupadoId === item.notificacionId}
              onCambiarEstado={(estado) => onCambiarEstado(item, estado)}
              botonTomada="primary"
            />
          </li>
        ))}
      </ul>

      {hayMas ? (
        <Link to="/recordatorios" className="font-semibold text-heading underline underline-offset-4">
          {verLos(agendaDeHoy.length)}
        </Link>
      ) : null}
    </section>
  )
}
