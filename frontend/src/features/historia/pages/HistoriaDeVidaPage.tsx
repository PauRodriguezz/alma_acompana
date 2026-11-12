import { useEffect, useState, type ReactNode } from "react"
import { useNavigate } from "react-router-dom"

import { TituloConFoco } from "@/components/alma/titulo-con-foco"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { useAccesibilidad } from "@/features/accesibilidad/useAccesibilidad"
import { useChat } from "@/features/chat/useChat"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO } from "@/lib/mensajes"
import { useEsEscritorio } from "@/lib/useEsEscritorio"
import { cn } from "@/lib/utils"

import { agruparPorEtapa } from "../agruparPorEtapa"
import { BotonDescargarMapa } from "../components/BotonDescargarMapa"
import { ListaDeEtapas } from "../components/ListaDeEtapas"
import { MapaDeVida } from "../components/MapaDeVida"
import { PanelDeEtapa } from "../components/PanelDeEtapa"
import { TarjetaProximaParada } from "../components/TarjetaProximaParada"
import type { IdEtapa } from "../etapas"
import "../mapa.css"
import { AVISO_TOCAR_BANDERA, SIN_RECUERDOS, SUBTITULO_HISTORIA, TITULO_HISTORIA_DE_VIDA } from "../mensajes"
import { RUTA_CHAT } from "../rutas"
import { useEstadoDelGuion, useRecuerdos } from "../useHistoria"

/**
 * Historia de vida (CU-04; contrato del mapa de vida, 2.1 a 2.4): el mapa por etapas, con el panel de la etapa
 * elegida. Con letra Grande o Muy grande el mapa pasa a ser una lista. La tarjeta "Próxima parada" va arriba.
 * Con la persona sin recuerdos se muestra el estado vacío y el mapa con las etapas vacías.
 */
export function HistoriaDeVidaPage() {
  const navegar = useNavigate()
  const esEscritorio = useEsEscritorio()
  const { preferencias } = useAccesibilidad()
  const recuerdos = useRecuerdos()
  const estado = useEstadoDelGuion()
  const { abrirPreguntaDelGuion, ocupadoGuion } = useChat()
  const [abriendo, setAbriendo] = useState(false)
  const [errorAlAbrir, setErrorAlAbrir] = useState<string | null>(null)
  const [mensajeFinal, setMensajeFinal] = useState<string | null>(null)
  const [elegida, setElegida] = useState<IdEtapa | null>(null)
  // El aviso ("Tocá una bandera…" y los puntos que laten) dura hasta la primera parada tocada en esta visita.
  const [tocoUnaParada, setTocoUnaParada] = useState(false)
  const [errorDeDescarga, setErrorDeDescarga] = useState<string | null>(null)

  const grupos = recuerdos.data ? agruparPorEtapa(recuerdos.data.recuerdos) : []
  // En escritorio no hay hoja: al entrar queda elegida la primera etapa con recuerdos.
  const idActivo: IdEtapa | null =
    elegida ?? (esEscritorio ? (grupos.find((g) => g.recuerdos.length > 0)?.etapa.id ?? null) : null)
  const grupoActivo = grupos.find((g) => g.etapa.id === idActivo) ?? null
  const hojaAbierta = !esEscritorio && grupoActivo !== null
  // En escritorio ya hay una etapa elegida al entrar, así que el aviso es solo de celular.
  const avisoActivo = !tocoUnaParada && !esEscritorio

  // En celular, al abrir la hoja el mapa se desplaza para que la parada quede por encima de ella.
  useEffect(() => {
    if (!hojaAbierta || elegida === null) return
    const parada = document.querySelector<HTMLElement>(`[data-etapa="${elegida}"]`)
    if (!parada) return
    const reducir = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    const arriba = parada.getBoundingClientRect().top - window.innerHeight * 0.12
    window.scrollBy({ top: arriba, behavior: reducir ? "auto" : "smooth" })
  }, [hojaAbierta, elegida])

  function elegirEtapa(id: IdEtapa) {
    setTocoUnaParada(true)
    setElegida(id)
  }

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
    const hayPendiente = (estado.data?.pendientes ?? 0) > 0
    const conLetraGrande = preferencias.tamanioLetra !== "NORMAL"

    contenido = (
      <>
        {recuerdos.data.recuerdos.length === 0 ? <Alert variant="info">{SIN_RECUERDOS}</Alert> : null}
        {errorAlAbrir ? <Alert variant="error">{errorAlAbrir}</Alert> : null}
        {mensajeFinal ? <Alert variant="info">{mensajeFinal}</Alert> : null}
        {hayPendiente ? (
          <TarjetaProximaParada onSeguir={seguirEscribiendo} deshabilitado={abriendo || ocupadoGuion} />
        ) : null}

        <div className="lg:grid lg:grid-cols-[minmax(0,28rem)_minmax(0,1fr)] lg:items-start lg:gap-8">
          {conLetraGrande ? (
            <ListaDeEtapas etapas={grupos} elegida={idActivo} onElegir={elegirEtapa} />
          ) : (
            <MapaDeVida etapas={grupos} elegida={idActivo} conAviso={avisoActivo} onElegir={elegirEtapa} />
          )}
          {esEscritorio && grupoActivo ? <PanelDeEtapa grupo={grupoActivo} modo="columna" /> : null}
        </div>

        {hojaAbierta ? (
          <PanelDeEtapa key={grupoActivo.etapa.id} grupo={grupoActivo} modo="hoja" onCerrar={() => setElegida(null)} />
        ) : null}

        {avisoActivo ? (
          <p className="sticky bottom-[calc(var(--nav-inferior)+env(safe-area-inset-bottom)+0.75rem)] z-10 mx-auto w-fit max-w-full rounded-full bg-primary px-5 py-2 text-center font-semibold text-primary-foreground shadow-md mapa-flotar lg:bottom-4">
            {AVISO_TOCAR_BANDERA}
          </p>
        ) : null}
      </>
    )
  }

  return (
    <div className={cn("flex flex-col gap-4 lg:max-w-[calc(960rem/17)]", hojaAbierta && "pb-[55dvh]")}>
      <header className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2">
        <div className="flex min-w-0 flex-col gap-1">
          <TituloConFoco>{TITULO_HISTORIA_DE_VIDA}</TituloConFoco>
          <p className="text-muted-foreground">{SUBTITULO_HISTORIA}</p>
        </div>
        {recuerdos.isSuccess ? <BotonDescargarMapa grupos={grupos} onError={setErrorDeDescarga} /> : null}
      </header>
      {errorDeDescarga ? <Alert variant="error">{errorDeDescarga}</Alert> : null}
      {contenido}
    </div>
  )
}
