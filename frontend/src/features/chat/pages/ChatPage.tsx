import { useEffect, useRef } from "react"

import { MensajeAlma } from "@/components/alma/mensaje-alma"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { BurbujaAnimo, EnlaceCambiarAnimo, WidgetAnimo } from "@/features/bienestar"
import { TarjetaParaHoy, useAgendaDeHoy, useCambioDeEstado } from "@/features/recordatorios"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO } from "@/lib/mensajes"
import { useAhora } from "@/lib/useAhora"

import { AccionesDelGuion } from "../components/AccionesDelGuion"
import { CampoMensaje } from "../components/CampoMensaje"
import { MensajeGuion } from "../components/MensajeGuion"
import { MensajePersona } from "../components/MensajePersona"
import { MensajeSinPendientes } from "../components/MensajeSinPendientes"
import { SaludoDeAlma } from "../components/SaludoDeAlma"
import { ALMA_PENSANDO, CONVERSACION_CON_ALMA } from "../mensajes"
import { useChat } from "../useChat"

/**
 * Pantalla del chat con Alma (CU-02), la ruta "/". Va dentro de LayoutPersona, que pone el <main> y la
 * navegación. De arriba hacia abajo: el saludo, la tarjeta "Para hoy" (CU-09), el ánimo (CU-01), la
 * conversación, el indicador de espera, el error y el campo de mensaje, fijo al pie (contrato, 11.2).
 * El estado vive en el ChatProvider, así que al ir a otra pantalla y volver todo sigue igual.
 */
export function ChatPage() {
  const chat = useChat()

  const ahora = useAhora()
  const agendaDeHoy = useAgendaDeHoy()
  const { cambiar, ocupadoId, anuncio, error: errorAgenda } = useCambioDeEstado()

  // Al aparecer un mensaje, el indicador o el error, la pantalla se desplaza para dejarlo a la vista.
  // No se desplaza al entrar: solo cuando algo cambia.
  const finDeLaConversacion = useRef<HTMLDivElement>(null)
  // La huella cambia con cada mensaje nuevo, con el indicador de espera y con el error.
  const huella = `${chat.mensajes.length}-${chat.pendiente}-${chat.error !== null}-${chat.objetivoActivo?.tipo}-${chat.cierrePendiente}-${chat.invitacionPendiente}`
  const huellaAnterior = useRef(huella)
  useEffect(() => {
    if (huella === huellaAnterior.current) return
    huellaAnterior.current = huella
    const reducirMovimiento = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    finDeLaConversacion.current?.scrollIntoView({
      behavior: reducirMovimiento ? "auto" : "smooth",
      block: "end",
    })
  }, [huella])

  // Con ánimo elegido: la burbuja y la pregunta de Alma. El widget se ve mientras no se eligió nada o
  // mientras se está cambiando, pero nunca después del primer mensaje (contrato de ánimo, 5.4 y 5.6).
  const mostrarWidget = !chat.yaEscribio && (!chat.animo || chat.cambiandoAnimo)
  const mostrarEnlace = Boolean(chat.animo) && !chat.yaEscribio && !chat.cambiandoAnimo

  return (
    <div className="flex min-h-[calc(100dvh-var(--nav-inferior)-env(safe-area-inset-bottom)-var(--page-top)-calc(84rem/17))] w-full flex-col gap-4 lg:min-h-[calc(100dvh-var(--page-top)-calc(24rem/17))] lg:max-w-(--narrow-max)">
      <SaludoDeAlma />

      <p role="status" className="sr-only">
        {anuncio}
      </p>
      {agendaDeHoy.error ? <Alert variant="error">{mensajeDeError(agendaDeHoy.error)}</Alert> : null}
      {errorAgenda ? <Alert variant="error">{mensajeDeError(errorAgenda)}</Alert> : null}
      <TarjetaParaHoy
        agendaDeHoy={agendaDeHoy.data ?? []}
        ahora={ahora}
        ocupadoId={ocupadoId}
        onCambiarEstado={cambiar}
      />

      <p role="status" className="sr-only">
        {chat.anuncioAnimo}
      </p>
      {chat.animo ? (
        <>
          <BurbujaAnimo nivelAnimo={chat.animo.nivelAnimo} />
          <MensajeAlma role="status">
            <p>{chat.animo.preguntaContextual}</p>
          </MensajeAlma>
        </>
      ) : null}
      {mostrarEnlace ? <EnlaceCambiarAnimo onClick={chat.empezarACambiarAnimo} /> : null}
      {mostrarWidget ? <WidgetAnimo registroId={chat.animo?.id} onRegistrado={chat.alRegistrarAnimo} /> : null}

      <div role="log" aria-label={CONVERSACION_CON_ALMA} className="flex flex-col gap-4">
        {chat.mensajes.map((mensaje) =>
          mensaje.rol === "PERSONA" ? (
            <MensajePersona key={mensaje.id} texto={mensaje.texto} />
          ) : mensaje.tipo === "SIN_PENDIENTES" ? (
            <MensajeSinPendientes key={mensaje.id} texto={mensaje.texto} />
          ) : mensaje.tipo === "GUION" ? (
            <MensajeGuion key={mensaje.id} texto={mensaje.texto} pregunta={mensaje.preguntaDestacada} />
          ) : (
            <MensajeAlma key={mensaje.id}>
              <p className="break-words whitespace-pre-line">{mensaje.texto}</p>
            </MensajeAlma>
          )
        )}
      </div>

      <AccionesDelGuion />

      {chat.pendiente ? (
        <MensajeAlma role="status">
          <p>{ALMA_PENSANDO}</p>
        </MensajeAlma>
      ) : null}

      {chat.error ? (
        <div className="flex flex-col items-start gap-3">
          <Alert variant="error" className="w-full">
            {chat.error}
          </Alert>
          {chat.errorReintentable ? (
            <Button variant="outline" onClick={chat.reintentar} aria-disabled={chat.pendiente}>
              {ACCION_PROBAR_DE_NUEVO}
            </Button>
          ) : null}
        </div>
      ) : null}

      {/* En celular, el botón "Seguir mi historia de vida" flota arriba del campo: este espacio deja el último mensaje a la vista. */}
      {chat.mostrarBotonSeguirHistoria ? <div aria-hidden="true" className="h-[calc(64rem/17)] shrink-0 lg:hidden" /> : null}

      {/* Detrás del campo fijo y de la navegación: el margen deja el último mensaje a la vista. */}
      <div
        ref={finDeLaConversacion}
        aria-hidden="true"
        className="scroll-mb-[calc(var(--nav-inferior)+env(safe-area-inset-bottom)+calc(96rem/17))]"
      />

      <CampoMensaje />
    </div>
  )
}
