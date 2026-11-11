import { SIN_FECHA } from "../mensajes"
import type { RecuerdoResumen } from "../tipos"
import { ParadaDeRecuerdo } from "./ParadaDeRecuerdo"

/** Los recuerdos sin año (contrato, 2.6): debajo del camino, en el orden en que llegan del back. */
export function SeccionSinFecha({ recuerdos }: { recuerdos: RecuerdoResumen[] }) {
  if (recuerdos.length === 0) return null
  return (
    <section aria-labelledby="historia-sin-fecha" className="flex flex-col gap-3">
      <h2 id="historia-sin-fecha" className="text-h3 lg:text-h2-desktop">
        {SIN_FECHA}
      </h2>
      <ul className="grid gap-3 lg:grid-cols-3">
        {recuerdos.map((recuerdo) => (
          <li key={recuerdo.id}>
            <ParadaDeRecuerdo recuerdo={recuerdo} />
          </li>
        ))}
      </ul>
    </section>
  )
}
