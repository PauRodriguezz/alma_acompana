import { useState } from "react"

import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { RUTA_JUEGOS } from "@/features/entretenimiento/rutas"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"
import { ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO } from "@/lib/mensajes"

import { EjercicioEnJuego } from "../components/EjercicioEnJuego"
import { FinalRonda } from "../components/FinalRonda"
import { ERROR_CARGAR_JUEGO, SIN_JUEGOS, TITULO_JUEGOS, VOLVER } from "../mensajes"
import { useSiguienteEjercicio } from "../useJuego"

/**
 * El juego (contrato, 10.2): ejercicio, respuesta y final son estados de esta misma pantalla.
 * Salir en medio de un ejercicio no llama a nada: el próximo `siguiente` lo vuelve a calcular el back.
 */
export function JuegoPage() {
  const siguiente = useSiguienteEjercicio()
  // Con rondaCompleta, "Continuar" lleva al final (jugadosHoy); si no, pide el siguiente.
  const [finalConJugados, setFinalConJugados] = useState<number | null>(null)

  if (finalConJugados !== null) {
    return (
      <FinalRonda
        jugadosHoy={finalConJugados}
        onSeguirJugando={() => {
          setFinalConJugados(null)
          siguiente.refetch()
        }}
      />
    )
  }

  // También mientras se pide el próximo: no se deja a la vista el ejercicio anterior.
  if (siguiente.isFetching || (siguiente.isPending && !siguiente.isError)) {
    return (
      <p role="status" className="text-muted-foreground">
        {ESTADO_CARGANDO}
      </p>
    )
  }

  if (siguiente.isError) {
    const sinJuegos = siguiente.error instanceof ApiError && siguiente.error.status === 404
    return (
      <div className="flex flex-col gap-4 lg:max-w-(--narrow-max)">
        <EnlaceVolver to={RUTA_JUEGOS} texto={VOLVER} />
        <h1 className="lg:text-h1-desktop">{TITULO_JUEGOS}</h1>
        {sinJuegos ? (
          <Alert variant="info">{SIN_JUEGOS}</Alert>
        ) : (
          <>
            <Alert variant="error">{mensajeErrorAlCargar(siguiente.error)}</Alert>
            <Button variant="outline" className="self-start" onClick={() => siguiente.refetch()}>
              {ACCION_PROBAR_DE_NUEVO}
            </Button>
          </>
        )}
      </div>
    )
  }

  return (
    <EjercicioEnJuego
      key={siguiente.dataUpdatedAt}
      datos={siguiente.data}
      onContinuar={(resultado) => {
        if (resultado.rondaCompleta) setFinalConJugados(resultado.jugadosHoy)
        else siguiente.refetch()
      }}
    />
  )
}

/** La sesión vencida y la falta de permiso conservan su texto; el resto, "No pudimos cargar el juego". */
function mensajeErrorAlCargar(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    return mensajeDeError(error)
  }
  return ERROR_CARGAR_JUEGO
}
