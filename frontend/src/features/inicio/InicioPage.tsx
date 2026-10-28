import { Alert } from "@/components/ui/alert"
import { useAuth } from "@/features/auth/useAuth"
import { TarjetaParaHoy } from "@/features/recordatorios/TarjetaParaHoy"
import { useAhora } from "@/features/recordatorios/useAhora"
import { useCambioDeEstado } from "@/features/recordatorios/useCambioDeEstado"
import { useAgendaDeHoy } from "@/features/recordatorios/useRecordatorios"
import { mensajeDeError } from "@/lib/errores"

// Pantalla provisoria: se reemplaza cuando exista el Inicio con el chat.
// Va dentro de LayoutPersona, que pone el <main> y la navegación; cerrar sesión está en Perfil.
export function InicioPage() {
  const { usuario } = useAuth()

  // PROVISORIO (CU-09): acá se monta la tarjeta "Para hoy" para poder probar el aviso. El módulo
  // de Chat la va a reemplazar: va dentro de su saludo (CU-02), con el mismo useAgendaDeHoy.
  const ahora = useAhora()
  const agendaDeHoy = useAgendaDeHoy()
  const { cambiar, ocupadoId, anuncio, error } = useCambioDeEstado()

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
      </div>
    </div>
  )
}
