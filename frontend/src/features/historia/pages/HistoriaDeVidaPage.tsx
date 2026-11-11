import { useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"

import { TituloConFoco } from "@/components/alma/titulo-con-foco"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { useChat } from "@/features/chat/useChat"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO } from "@/lib/mensajes"
import { useEsEscritorio } from "@/lib/useEsEscritorio"

import { BOTON_TEXTO_LARGO } from "../estilos"
import { CaminoDeRecuerdos, type ParadaNumerada } from "../components/CaminoDeRecuerdos"
import { SeccionSinFecha } from "../components/SeccionSinFecha"
import { TarjetaProximaParada } from "../components/TarjetaProximaParada"
import {
  OCULTAR_RECUERDOS_ANTERIORES,
  SIN_RECUERDOS,
  TITULO_HISTORIA_DE_VIDA,
  verRecuerdosAnteriores,
} from "../mensajes"
import { RUTA_CHAT } from "../rutas"
import { useEstadoDelGuion, useRecuerdos } from "../useHistoria"

/** En celular se ven las 3 paradas más recientes (contrato, 2.6). */
const PARADAS_VISIBLES_EN_CELULAR = 3

/**
 * Historia de vida (CU-04; Figma 31:585 y 46:1157): el camino de paradas numeradas, la tarjeta
 * "Próxima parada" al final si hay una pregunta pendiente y, debajo, los recuerdos sin año.
 * Con la persona sin recuerdos se muestra el estado vacío, y la tarjeta igual si corresponde.
 */
export function HistoriaDeVidaPage() {
  const navegar = useNavigate()
  const esEscritorio = useEsEscritorio()
  const recuerdos = useRecuerdos()
  const estado = useEstadoDelGuion()
  const { abrirPreguntaDelGuion, ocupadoGuion } = useChat()
  const [verTodos, setVerTodos] = useState(false)
  const [abriendo, setAbriendo] = useState(false)
  const [errorAlAbrir, setErrorAlAbrir] = useState<string | null>(null)
  const [mensajeFinal, setMensajeFinal] = useState<string | null>(null)

  /**
   * "Seguir escribiendo" (5.4): el chat pide la próxima pregunta y la muestra como mensaje de Alma; después
   * se navega a "/". Si no quedan preguntas (otra pestaña las respondió) se muestra el mensaje final acá y la
   * pantalla se refresca; si el pedido falla, se avisa acá y no se navega.
   */
  async function seguirEscribiendo() {
    setAbriendo(true)
    setErrorAlAbrir(null)
    try {
      const apertura = await abrirPreguntaDelGuion()
      if (apertura.resultado === "SIN_PENDIENTES") {
        setMensajeFinal(apertura.mensajeFinal)
        return
      }
      navegar(RUTA_CHAT)
    } catch (e) {
      setErrorAlAbrir(mensajeDeError(e))
    } finally {
      setAbriendo(false)
    }
  }

  let contenido: ReactNode
  if (recuerdos.isPending) {
    contenido = (
      <p role="status" className="text-muted-foreground">
        {ESTADO_CARGANDO}
      </p>
    )
  } else if (recuerdos.isError) {
    contenido = (
      <div className="flex flex-col gap-4">
        <Alert variant="error">{mensajeDeError(recuerdos.error)}</Alert>
        <Button variant="outline" className="self-start" onClick={() => recuerdos.refetch()}>
          {ACCION_PROBAR_DE_NUEVO}
        </Button>
      </div>
    )
  } else {
    const todos = recuerdos.data.recuerdos
    const conAnio = todos.filter((r) => r.anio !== null)
    const sinAnio = todos.filter((r) => r.anio === null)
    // La numeración es la posición cronológica y no cambia al ocultar las más viejas.
    const numeradas: ParadaNumerada[] = conAnio.map((recuerdo, i) => ({ numero: i + 1, recuerdo }))
    const anteriores = Math.max(0, numeradas.length - PARADAS_VISIBLES_EN_CELULAR)
    const hayAnteriores = !esEscritorio && anteriores > 0
    const visibles = hayAnteriores && !verTodos ? numeradas.slice(anteriores) : numeradas
    const hayPendiente = (estado.data?.pendientes ?? 0) > 0

    contenido = (
      <>
        {hayAnteriores ? (
          <Button
            variant="secondary"
            className={`w-full ${BOTON_TEXTO_LARGO}`}
            onClick={() => setVerTodos((actual) => !actual)}
          >
            {verTodos ? OCULTAR_RECUERDOS_ANTERIORES : verRecuerdosAnteriores(anteriores)}
          </Button>
        ) : null}

        {todos.length === 0 ? <Alert variant="info">{SIN_RECUERDOS}</Alert> : null}
        {errorAlAbrir ? <Alert variant="error">{errorAlAbrir}</Alert> : null}
        {mensajeFinal ? <Alert variant="info">{mensajeFinal}</Alert> : null}

        <CaminoDeRecuerdos
          paradas={visibles}
          proximaParada={
            hayPendiente ? (
              <TarjetaProximaParada onSeguir={seguirEscribiendo} deshabilitado={abriendo || ocupadoGuion} />
            ) : undefined
          }
        />

        <SeccionSinFecha recuerdos={sinAnio} />
      </>
    )
  }

  return (
    <div className="flex flex-col gap-4 lg:max-w-[calc(960rem/17)]">
      <TituloConFoco>{TITULO_HISTORIA_DE_VIDA}</TituloConFoco>
      {contenido}
    </div>
  )
}
