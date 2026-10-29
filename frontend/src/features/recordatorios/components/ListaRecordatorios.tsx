import type { ReactNode } from "react"

import { hoyArgentina, sumarDias } from "@/lib/fechas"
import { Alert } from "@/components/ui/alert"
import { mensajeDeError } from "@/lib/errores"
import { useAhora } from "@/lib/useAhora"

import {
  CARGANDO,
  SIN_CITAS,
  SIN_MEDICACION_HOY,
  SIN_MEDICACIONES,
  TITULO_CITAS,
  TITULO_MEDICACION_DE_HOY,
  TITULO_MIS_MEDICACIONES,
} from "../mensajes"
import type { Medicacion } from "../tipos"
import {
  useCambioDeEstado,
  useAgenda,
  useAgendaDeHoy,
  useRecordatoriosLista,
} from "../useRecordatorios"

import { TarjetaAviso, TarjetaMedicacion } from "./Tarjetas"

const DIAS_DE_CITAS_PROXIMAS = 30

// Las secciones ya no van en un contenedor blanco: cada tarjeta tiene su propio borde.
function Seccion({ titulo, id, children }: { titulo: string; id: string; children: ReactNode }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-3">
      <h2 id={id} className="text-h3">
        {titulo}
      </h2>
      {children}
    </section>
  )
}

/** Cargando, error o el contenido de una consulta, con el estado anunciado al lector de pantalla. */
function Estado({
  cargando,
  error,
  children,
}: {
  cargando: boolean
  error: unknown
  children: ReactNode
}) {
  if (cargando) {
    return (
      <p role="status" className="py-3 text-muted-foreground">
        {CARGANDO}
      </p>
    )
  }
  if (error) {
    return <Alert variant="error">{mensajeDeError(error)}</Alert>
  }
  return <>{children}</>
}

/** Lista (Figma): medicación de hoy, citas próximas y todas mis medicaciones. */
export function ListaRecordatorios() {
  const ahora = useAhora()
  const hoy = hoyArgentina()

  const agendaDeHoy = useAgendaDeHoy()
  const citas = useAgenda(hoy, sumarDias(hoy, DIAS_DE_CITAS_PROXIMAS), "CITA")
  const medicaciones = useRecordatoriosLista("MEDICACION")

  const { cambiar, ocupadoId, anuncio, error: errorEstado } = useCambioDeEstado()

  const medicacionDeHoy = (agendaDeHoy.data ?? []).filter((i) => i.recordatorio.tipo === "MEDICACION")
  const misMedicaciones = [...((medicaciones.data ?? []) as Medicacion[])].sort((a, b) =>
    a.nombreMedicamento.localeCompare(b.nombreMedicamento, "es")
  )

  return (
    <div className="flex flex-col gap-6">
      {/* Aviso para el lector de pantalla al marcar o deshacer una toma */}
      <p role="status" className="sr-only">
        {anuncio}
      </p>
      {errorEstado ? <Alert variant="error">{mensajeDeError(errorEstado)}</Alert> : null}

      <div className="flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:items-start lg:gap-x-(--slot)">
        <Seccion id="lista-medicacion-hoy" titulo={TITULO_MEDICACION_DE_HOY}>
          <Estado cargando={agendaDeHoy.isPending} error={agendaDeHoy.error}>
            {medicacionDeHoy.length === 0 ? (
              <p className="py-3 text-muted-foreground">{SIN_MEDICACION_HOY}</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {medicacionDeHoy.map((item) => (
                  <li key={item.notificacionId}>
                    <TarjetaAviso
                      item={item}
                      ahora={ahora}
                      ocupado={ocupadoId === item.notificacionId}
                      onCambiarEstado={(estado) => cambiar(item, estado)}
                    />
                  </li>
                ))}
              </ul>
            )}
          </Estado>
        </Seccion>

        <Seccion id="lista-citas" titulo={TITULO_CITAS}>
          <Estado cargando={citas.isPending} error={citas.error}>
            {(citas.data ?? []).length === 0 ? (
              <p className="py-3 text-muted-foreground">{SIN_CITAS}</p>
            ) : (
              <ul className="flex flex-col gap-3">
                {(citas.data ?? []).map((item) => (
                  <li key={item.notificacionId}>
                    <TarjetaAviso item={item} ahora={ahora} />
                  </li>
                ))}
              </ul>
            )}
          </Estado>
        </Seccion>
      </div>

      <Seccion id="lista-mis-medicaciones" titulo={TITULO_MIS_MEDICACIONES}>
        <Estado cargando={medicaciones.isPending} error={medicaciones.error}>
          {misMedicaciones.length === 0 ? (
            <p className="py-3 text-muted-foreground">{SIN_MEDICACIONES}</p>
          ) : (
            <ul className="flex flex-col gap-3 lg:grid lg:grid-cols-2 lg:gap-x-(--slot)">
              {misMedicaciones.map((medicacion) => (
                <li key={medicacion.id}>
                  <TarjetaMedicacion medicacion={medicacion} />
                </li>
              ))}
            </ul>
          )}
        </Estado>
      </Seccion>
    </div>
  )
}
