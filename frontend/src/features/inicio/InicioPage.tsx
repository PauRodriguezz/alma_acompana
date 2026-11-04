import { useState } from "react"

import { MensajeAlma } from "@/components/alma/mensaje-alma"
import { Alert } from "@/components/ui/alert"
import { useAuth } from "@/features/auth/useAuth"
import {
  BurbujaAnimo,
  EnlaceCambiarAnimo,
  WidgetAnimo,
  anuncioCorregido,
  anuncioRegistrado,
  type RegistroAnimo,
} from "@/features/bienestar"
import { TarjetaParaHoy, useAgendaDeHoy, useCambioDeEstado } from "@/features/recordatorios"
import { mensajeDeError } from "@/lib/errores"
import { useAhora } from "@/lib/useAhora"

// Pantalla provisoria: se reemplaza cuando exista el Inicio con el chat.
// Va dentro de LayoutPersona, que pone el <main> y la navegación; cerrar sesión está en Perfil.
export function InicioPage() {
  const { usuario } = useAuth()

  // PROVISORIO (CU-09): acá se monta la tarjeta "Para hoy" para poder probar el aviso. El módulo
  // de Chat la va a reemplazar: va dentro de su saludo (CU-02), con el mismo useAgendaDeHoy.
  const ahora = useAhora()
  const agendaDeHoy = useAgendaDeHoy()
  const { cambiar, ocupadoId, anuncio, error } = useCambioDeEstado()

  // PROVISORIO (CU-01): acá se monta el widget de ánimo para poder probarlo de punta a punta.
  // feature/chat lo reemplaza: lo va a montar debajo del saludo (CU-02) y va a quitar el enlace
  // "Cambiar cómo me siento" cuando la persona mande su primer mensaje (acá no hay dónde escribir).
  // Todo vive en memoria: al recargar, el widget vuelve a aparecer y la próxima elección crea un registro nuevo.
  const [animo, setAnimo] = useState<RegistroAnimo | null>(null)
  const [cambiandoAnimo, setCambiandoAnimo] = useState(false)
  const [anuncioAnimo, setAnuncioAnimo] = useState("")

  function alRegistrarAnimo(registro: RegistroAnimo) {
    // Si ya había un registro, la elección lo corrigió (PUT): no se agrega nada nuevo, se reemplaza.
    setAnuncioAnimo(
      animo ? anuncioCorregido(registro.nivelAnimo) : anuncioRegistrado(registro.nivelAnimo)
    )
    setAnimo(registro)
    setCambiandoAnimo(false)
  }

  return (
    <div className="flex flex-col items-start gap-4">
      <h1>Inicio</h1>
      <p>Usuario: {usuario?.nombreUsuario}</p>

      <div className="flex w-full flex-col gap-4 lg:max-w-(--narrow-max)">
        <p role="status" className="sr-only">
          {anuncio}
        </p>
        {agendaDeHoy.error ? <Alert variant="error">{mensajeDeError(agendaDeHoy.error)}</Alert> : null}
        {error ? <Alert variant="error">{mensajeDeError(error)}</Alert> : null}
        <TarjetaParaHoy
          agendaDeHoy={agendaDeHoy.data ?? []}
          ahora={ahora}
          ocupadoId={ocupadoId}
          onCambiarEstado={cambiar}
        />

        <p role="status" className="sr-only">
          {anuncioAnimo}
        </p>
        {animo ? (
          <>
            <BurbujaAnimo nivelAnimo={animo.nivelAnimo} />
            <MensajeAlma role="status">
              <p>{animo.preguntaContextual}</p>
            </MensajeAlma>
            {cambiandoAnimo ? null : <EnlaceCambiarAnimo onClick={() => setCambiandoAnimo(true)} />}
          </>
        ) : null}
        {!animo || cambiandoAnimo ? (
          <WidgetAnimo registroId={animo?.id} onRegistrado={alRegistrarAnimo} />
        ) : null}
      </div>
    </div>
  )
}
