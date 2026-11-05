import { useRef, useState, type ReactNode } from "react"
import { useMutation } from "@tanstack/react-query"

import { anuncioCorregido, anuncioRegistrado, type RegistroAnimo } from "@/features/bienestar"
import { useAuth } from "@/features/auth/useAuth"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"

import { enviarMensaje } from "./api"
import { ChatContexto, type ChatContextoValor } from "./contexto"
import { LIMITE_TURNOS, type MensajeChat, type TurnoChat } from "./tipos"

/**
 * La conversación se sostiene por encima de las pantallas, así sobrevive a ir a Recordatorios o
 * Perfil y volver. La `key` es el id de la persona: al cerrar sesión o ingresar con otra cuenta,
 * React vuelve a crear el estado y empieza de cero. Nada se guarda en el navegador (5.8).
 */
export function ChatProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  return <EstadoDelChat key={usuario?.id ?? "sin-sesion"}>{children}</EstadoDelChat>
}

/** Con 503 el texto es el que manda el back (6.4); con cualquier otro fallo, el general. */
function textoDeError(error: unknown): string {
  if (error instanceof ApiError && error.status === 503 && error.detalle) return error.detalle
  return mensajeDeError(error)
}

/**
 * La conversación que viaja con el mensaje nuevo (11.4, paso 3): la pregunta contextual de Alma
 * (solo si hay ánimo, el vigente en este momento) y los mensajes, sin el mensaje nuevo. Hasta los
 * últimos 10. No se mandan el saludo, la pregunta del widget ni la burbuja del ánimo.
 */
function armarConversacion(animo: RegistroAnimo | null, mensajes: MensajeChat[]): TurnoChat[] {
  const turnos: TurnoChat[] = []
  if (animo) turnos.push({ rol: "ALMA", texto: animo.preguntaContextual })
  for (const mensaje of mensajes) turnos.push({ rol: mensaje.rol, texto: mensaje.texto })
  return turnos.slice(-LIMITE_TURNOS)
}

function EstadoDelChat({ children }: { children: ReactNode }) {
  const [animo, setAnimo] = useState<RegistroAnimo | null>(null)
  const [cambiandoAnimo, setCambiandoAnimo] = useState(false)
  const [yaEscribio, setYaEscribio] = useState(false)
  const [mensajes, setMensajes] = useState<MensajeChat[]>([])
  const [pendiente, setPendiente] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [ultimoMensaje, setUltimoMensaje] = useState("")
  const [borrador, setBorrador] = useState("")
  const [anuncioAnimo, setAnuncioAnimo] = useState("")

  // Evita que un doble toque mande dos veces (el estado `pendiente` se actualiza un instante después).
  const enviando = useRef(false)
  const proximoId = useRef(1)

  const mutacion = useMutation({
    mutationFn: ({ mensaje, conversacion }: { mensaje: string; conversacion: TurnoChat[] }) =>
      enviarMensaje(mensaje, conversacion),
  })

  async function enviar(textoIngresado: string) {
    const texto = textoIngresado.trim()
    if (!texto || enviando.current) return
    enviando.current = true

    // El ánimo vigente en este momento: no se guarda aparte.
    const conversacion = armarConversacion(animo, mensajes)
    const idPersona = proximoId.current++

    setMensajes((actuales) => [...actuales, { id: idPersona, rol: "PERSONA", texto }])
    // Se limpia el campo solo si todavía tiene lo que se manda (un reintento no pisa lo nuevo).
    setBorrador((actual) => (actual.trim() === texto ? "" : actual))
    setUltimoMensaje(texto)
    setError(null)
    setYaEscribio(true)
    // Si tenía abierto "Cambiar cómo me siento" y no eligió, el widget se cierra y queda el ánimo anterior.
    setCambiandoAnimo(false)
    setPendiente(true)

    try {
      const { respuesta } = await mutacion.mutateAsync({ mensaje: texto, conversacion })
      setMensajes((actuales) => [...actuales, { id: proximoId.current++, rol: "ALMA", texto: respuesta }])
    } catch (e) {
      setMensajes((actuales) => actuales.filter((mensaje) => mensaje.id !== idPersona))
      // El texto vuelve al campo solo si está vacío: si ya escribió otra cosa, no se pisa.
      setBorrador((actual) => (actual === "" ? texto : actual))
      setError(textoDeError(e))
    } finally {
      setPendiente(false)
      enviando.current = false
    }
  }

  const valor: ChatContextoValor = {
    animo,
    cambiandoAnimo,
    yaEscribio,
    mensajes,
    pendiente,
    error,
    borrador,
    anuncioAnimo,
    cambiarBorrador: setBorrador,
    enviar: (texto) => void enviar(texto),
    reintentar: () => void enviar(ultimoMensaje),
    alRegistrarAnimo: (registro) => {
      // Si ya había un registro, la elección lo corrigió (PUT): se reemplaza, no se agrega nada.
      setAnuncioAnimo(
        animo ? anuncioCorregido(registro.nivelAnimo) : anuncioRegistrado(registro.nivelAnimo)
      )
      setAnimo(registro)
      setCambiandoAnimo(false)
    },
    empezarACambiarAnimo: () => setCambiandoAnimo(true),
  }

  return <ChatContexto.Provider value={valor}>{children}</ChatContexto.Provider>
}
