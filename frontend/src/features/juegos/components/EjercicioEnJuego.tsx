import { useEffect, useRef, useState } from "react"

import { Alert } from "@/components/ui/alert"
import { mensajeDeError } from "@/lib/errores"

import { anuncioResultado } from "../mensajes"
import type { RespuestaCuerpo, ResultadoRespuesta, SiguienteEjercicio } from "../tipos"
import { useResponder } from "../useJuego"
import { BarraProgreso } from "./BarraProgreso"
import { ChipCategoria } from "./ChipCategoria"
import { OpcionesMultiples } from "./OpcionesMultiples"
import { PanelRespuesta } from "./PanelRespuesta"
import { PasosOrdenar } from "./PasosOrdenar"
import { TextoAMemorizar } from "./TextoAMemorizar"

/**
 * Un ejercicio, desde que aparece hasta "Continuar". JuegoPage lo monta con un key por cada pedido a
 * `siguiente`, así el estado (fase de memoria, opción elegida, resultado) arranca de cero aunque
 * se repita el mismo ejercicio (C.A. Nº1).
 */
export function EjercicioEnJuego({
  datos,
  onContinuar,
}: {
  datos: SiguienteEjercicio
  onContinuar: (resultado: ResultadoRespuesta) => void
}) {
  const { ejercicio, posicion, totalRonda } = datos
  const [memorizando, setMemorizando] = useState(ejercicio.textoAMemorizar !== null)
  const [elegidaId, setElegidaId] = useState<number | null>(null)
  const responder = useResponder()
  const resultado = responder.data
  // Mientras viaja la respuesta y después del resultado no se puede volver a responder
  // (contrato, pendiente 3: el back no tiene guarda contra el doble envío).
  const bloqueado = responder.isPending || resultado !== undefined

  // Al llegar el ejercicio, y al pasar de la memoria a la consigna, el foco va al título y la
  // pantalla vuelve arriba (para que se vea la barra de progreso).
  const tituloRef = useRef<HTMLHeadingElement>(null)
  useEffect(() => {
    window.scrollTo({ top: 0 })
    tituloRef.current?.focus({ preventScroll: true })
  }, [memorizando])

  function enviar(cuerpo: RespuestaCuerpo) {
    if (bloqueado) return
    responder.mutate(cuerpo)
  }

  return (
    <div className="flex flex-col gap-4 lg:max-w-(--narrow-max)">
      <BarraProgreso posicion={posicion} total={totalRonda} />
      <ChipCategoria categoria={ejercicio.categoria} />

      {memorizando && ejercicio.textoAMemorizar !== null ? (
        <TextoAMemorizar
          texto={ejercicio.textoAMemorizar}
          tituloRef={tituloRef}
          onRecordado={() => setMemorizando(false)}
        />
      ) : (
        <>
          <h1
            ref={tituloRef}
            tabIndex={-1}
            className="text-h2 wrap-break-word outline-none"
          >
            {ejercicio.consigna}
          </h1>

          {ejercicio.formato === "ORDENAR" ? (
            <PasosOrdenar
              pasos={ejercicio.opciones}
              bloqueados={bloqueado}
              respondido={resultado !== undefined}
              onListo={(ids) => enviar({ ejercicioId: ejercicio.id, ordenOpcionIds: ids })}
            />
          ) : (
            <OpcionesMultiples
              opciones={ejercicio.opciones}
              elegidaId={elegidaId}
              resultado={resultado}
              bloqueadas={bloqueado}
              onElegir={(id) => {
                setElegidaId(id)
                enviar({ ejercicioId: ejercicio.id, opcionId: id })
              }}
            />
          )}

          {responder.isError ? <Alert variant="error">{mensajeDeError(responder.error)}</Alert> : null}

          {resultado ? (
            <PanelRespuesta resultado={resultado} onContinuar={() => onContinuar(resultado)} />
          ) : null}
        </>
      )}

      {/* Región siempre montada: así el lector de pantalla anuncia el resultado al llegar */}
      <p role="status" className="sr-only">
        {resultado ? anuncioResultado(resultado.acierto, resultado.respuestaCorrecta) : ""}
      </p>
    </div>
  )
}
